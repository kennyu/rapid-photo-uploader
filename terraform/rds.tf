# RDS Database Configuration
# Modifies existing RDS instance to make it private and secure

# Make RDS instance private (not publicly accessible)
resource "aws_db_instance" "main" {
  identifier = var.existing_rds_identifier
  
  # Reference existing instance to avoid recreation
  allocated_storage     = data.aws_db_instance.main.allocated_storage
  engine                = data.aws_db_instance.main.engine
  engine_version        = data.aws_db_instance.main.engine_version
  instance_class        = data.aws_db_instance.main.db_instance_class
  db_name               = data.aws_db_instance.main.db_name
  username              = var.db_username
  password              = var.db_password
  
  # Security settings
  publicly_accessible    = false  # Make it private
  vpc_security_group_ids = [var.existing_rds_sg_id]
  db_subnet_group_name   = data.aws_db_instance.main.db_subnet_group
  
  # Backup and maintenance
  backup_retention_period      = 7
  backup_window                = "03:00-04:00"
  maintenance_window           = "Mon:04:00-Mon:05:00"
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  # Performance and monitoring
  performance_insights_enabled    = true
  performance_insights_retention_period = 7
  monitoring_interval             = 60
  monitoring_role_arn            = aws_iam_role.rds_monitoring.arn
  
  # Encryption
  storage_encrypted = true
  
  # Deletion protection
  deletion_protection = true
  skip_final_snapshot = false
  final_snapshot_identifier = "${var.existing_rds_identifier}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  
  # Apply changes immediately
  apply_immediately = true
  
  tags = {
    Name = "${var.project_name}-database"
  }
  
  lifecycle {
    ignore_changes = [
      # Ignore snapshot identifier changes
      final_snapshot_identifier,
      # Ignore minor version updates
      engine_version,
    ]
  }
}

# IAM role for RDS enhanced monitoring
resource "aws_iam_role" "rds_monitoring" {
  name = "${var.project_name}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

