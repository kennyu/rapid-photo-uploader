# Terraform Outputs

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.backend.id
}

output "ec2_public_ip" {
  description = "EC2 public IP address"
  value       = aws_eip.backend.public_ip
}

output "ec2_private_ip" {
  description = "EC2 private IP address"
  value       = aws_instance.backend.private_ip
}

output "backend_url" {
  description = "Backend API URL"
  value       = "http://${aws_eip.backend.public_ip}:8080"
}

output "health_check_url" {
  description = "Health check endpoint"
  value       = "http://${aws_eip.backend.public_ip}:8080/actuator/health"
}

output "ssh_command" {
  description = "SSH command to connect to EC2 instance"
  value       = "ssh -i ssh_key ec2-user@${aws_eip.backend.public_ip}"
}

output "rds_endpoint" {
  description = "RDS database endpoint"
  value       = data.aws_db_instance.main.endpoint
  sensitive   = true
}

output "rds_publicly_accessible" {
  description = "Whether RDS is publicly accessible (should be false)"
  value       = aws_db_instance.main.publicly_accessible
}

output "s3_bucket_name" {
  description = "S3 bucket name"
  value       = data.aws_s3_bucket.main.id
}

output "iam_role_arn" {
  description = "IAM role ARN for backend EC2"
  value       = aws_iam_role.backend.arn
}

output "security_group_id" {
  description = "Backend security group ID"
  value       = aws_security_group.backend.id
}

output "deployment_commands" {
  description = "Commands to deploy the application"
  value = <<-EOT
    # Build the JAR
    cd backend
    ./mvnw clean package -DskipTests
    
    # Upload to EC2
    scp -i terraform/ssh_key backend/target/rapid-photo-uploader-*.jar ec2-user@${aws_eip.backend.public_ip}:/tmp/app.jar
    
    # Deploy on EC2
    ssh -i terraform/ssh_key ec2-user@${aws_eip.backend.public_ip} << 'DEPLOY'
      sudo mv /tmp/app.jar /opt/rapidphoto/rapid-photo-uploader.jar
      sudo chown rapidphoto:rapidphoto /opt/rapidphoto/rapid-photo-uploader.jar
      sudo systemctl start rapidphoto-backend
      sudo systemctl status rapidphoto-backend
    DEPLOY
    
    # Test
    curl http://${aws_eip.backend.public_ip}:8080/actuator/health
  EOT
}

