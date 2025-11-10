# EC2 Backend Instance

# Reference existing SSH key pair
data "aws_key_pair" "backend" {
  key_name = "ken.ssh"
}

# User data script for EC2
locals {
  user_data = <<-EOF
    #!/bin/bash
    set -e
    
    # Update system
    yum update -y
    
    # Install Java 21
    yum install -y java-21-amazon-corretto-headless
    
    # Create application user
    useradd -m -s /bin/bash rapidphoto
    
    # Create directories
    mkdir -p /opt/rapidphoto
    mkdir -p /var/log/rapidphoto
    chown -R rapidphoto:rapidphoto /opt/rapidphoto /var/log/rapidphoto
    
    # Generate JWT secret
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    
    # Create systemd service
    cat > /etc/systemd/system/rapidphoto-backend.service << 'SVCEOF'
    [Unit]
    Description=RapidPhoto Backend Service
    After=network.target
    
    [Service]
    Type=simple
    User=rapidphoto
    Group=rapidphoto
    WorkingDirectory=/opt/rapidphoto
    ExecStart=/usr/bin/java -jar \
      -Xms512m \
      -Xmx1024m \
      -XX:+UseG1GC \
      /opt/rapidphoto/rapid-photo-uploader.jar
    
    Restart=always
    RestartSec=10
    StandardOutput=journal
    StandardError=journal
    
    Environment="AWS_REGION=${var.aws_region}"
    Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://${data.aws_db_instance.main.endpoint}/rapidphoto"
    Environment="SPRING_DATASOURCE_USERNAME=${var.db_username}"
    Environment="SPRING_DATASOURCE_PASSWORD=${var.db_password}"
    Environment="AWS_S3_BUCKET_NAME=${var.existing_s3_bucket}"
    Environment="JWT_SECRET=$JWT_SECRET"
    
    [Install]
    WantedBy=multi-user.target
    SVCEOF
    
    systemctl daemon-reload
    systemctl enable rapidphoto-backend
    
    echo "EC2 setup complete!" > /var/log/user-data.log
  EOF
}

# EC2 instance
resource "aws_instance" "backend" {
  ami           = data.aws_ami.amazon_linux_2023_arm.id
  instance_type = var.instance_type
  
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [aws_security_group.backend.id]
  iam_instance_profile        = aws_iam_instance_profile.backend.name
  key_name                    = data.aws_key_pair.backend.key_name
  associate_public_ip_address = true
  
  user_data                   = local.user_data
  user_data_replace_on_change = true
  
  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }
  
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"  # IMDSv2
    http_put_response_hop_limit = 1
  }
  
  monitoring = true
  
  tags = {
    Name = "${var.project_name}-backend-${var.environment}"
  }
  
  lifecycle {
    ignore_changes = [
      ami,  # Don't recreate instance on AMI updates
    ]
  }
}

# Elastic IP for stable address (optional but recommended)
resource "aws_eip" "backend" {
  instance = aws_instance.backend.id
  domain   = "vpc"
  
  tags = {
    Name = "${var.project_name}-backend-eip"
  }
}

