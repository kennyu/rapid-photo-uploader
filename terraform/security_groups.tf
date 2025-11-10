# Security Groups for EC2 Backend

# Security group for backend EC2 instance
resource "aws_security_group" "backend" {
  name        = "${var.project_name}-backend-sg"
  description = "Security group for ${var.project_name} backend EC2 instance"
  vpc_id      = var.existing_vpc_id

  tags = {
    Name = "${var.project_name}-backend-sg"
  }
}

# Allow SSH from your IP (optional)
resource "aws_vpc_security_group_ingress_rule" "backend_ssh" {
  count = var.enable_ssh_access ? 1 : 0

  security_group_id = aws_security_group.backend.id
  description       = "SSH access from allowed IP"
  
  from_port   = 22
  to_port     = 22
  ip_protocol = "tcp"
  cidr_ipv4   = local.ssh_cidr
}

# Allow backend API traffic (port 8080)
resource "aws_vpc_security_group_ingress_rule" "backend_api" {
  security_group_id = aws_security_group.backend.id
  description       = "Backend API access"
  
  from_port   = 8080
  to_port     = 8080
  ip_protocol = "tcp"
  cidr_ipv4   = "0.0.0.0/0"
}

# Allow all outbound traffic
resource "aws_vpc_security_group_egress_rule" "backend_egress" {
  security_group_id = aws_security_group.backend.id
  description       = "Allow all outbound traffic"
  
  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

# Update RDS security group to allow backend access
resource "aws_vpc_security_group_ingress_rule" "rds_from_backend" {
  security_group_id = var.existing_rds_sg_id
  description       = "PostgreSQL access from backend EC2"
  
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.backend.id
}

# Optional: Allow your IP to access RDS directly (for management)
resource "aws_vpc_security_group_ingress_rule" "rds_from_my_ip" {
  count = var.enable_ssh_access ? 1 : 0

  security_group_id = var.existing_rds_sg_id
  description       = "PostgreSQL access from management IP"
  
  from_port   = 5432
  to_port     = 5432
  ip_protocol = "tcp"
  cidr_ipv4   = local.ssh_cidr
}

