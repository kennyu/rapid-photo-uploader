# RapidPhoto Infrastructure - Main Configuration

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    http = {
      source  = "hashicorp/http"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Get current IP for SSH access
data "http" "my_ip" {
  url = "https://api.ipify.org"
}

locals {
  my_ip            = "${chomp(data.http.my_ip.response_body)}/32"
  ssh_cidr         = var.allowed_ssh_cidr != null ? var.allowed_ssh_cidr : local.my_ip
  account_id       = data.aws_caller_identity.current.account_id
}

data "aws_caller_identity" "current" {}

# Data sources for existing resources
data "aws_vpc" "main" {
  id = var.existing_vpc_id
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [var.existing_vpc_id]
  }
}

data "aws_db_instance" "main" {
  db_instance_identifier = var.existing_rds_identifier
}

data "aws_s3_bucket" "main" {
  bucket = var.existing_s3_bucket
}

# Get latest Amazon Linux 2023 ARM AMI
data "aws_ami" "amazon_linux_2023_arm" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-kernel-6.1-arm64"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

