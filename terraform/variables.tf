# Variables for RapidPhoto Infrastructure

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "rapidphoto"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t4g.small" # ARM-based, $12/month
}

variable "db_password" {
  description = "RDS database password"
  type        = string
  sensitive   = true
  default     = "RapidPhoto2024SecurePass!"
}

variable "db_username" {
  description = "RDS database username"
  type        = string
  default     = "postgres"
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH into EC2 (your IP)"
  type        = string
  # Will be populated by data source if not provided
  default     = null
}

variable "enable_ssh_access" {
  description = "Enable SSH access to EC2 instance"
  type        = bool
  default     = true
}

# Existing resource IDs
variable "existing_vpc_id" {
  description = "Existing VPC ID"
  type        = string
  default     = "vpc-00e884c2c3049dc60"
}

variable "existing_rds_identifier" {
  description = "Existing RDS instance identifier"
  type        = string
  default     = "rapidphoto-db"
}

variable "existing_s3_bucket" {
  description = "Existing S3 bucket name"
  type        = string
  default     = "rapidphoto-uploads-297721440242"
}

variable "existing_rds_sg_id" {
  description = "Existing RDS security group ID"
  type        = string
  default     = "sg-04bae24dd0a44ce57"
}

variable "subnet_id" {
  description = "Subnet ID for EC2 instance (must be public)"
  type        = string
  default     = "subnet-00f69fe6ab7d3030f" # us-east-1a
}

