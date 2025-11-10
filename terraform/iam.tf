# IAM Role and Policies for EC2 Backend

# IAM role for EC2 instance
resource "aws_iam_role" "backend" {
  name        = "${var.project_name}-backend-ec2-role"
  description = "IAM role for ${var.project_name} backend EC2 instance"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# IAM policy for S3 access
resource "aws_iam_role_policy" "backend_s3" {
  name = "${var.project_name}-s3-access"
  role = aws_iam_role.backend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3ObjectOperations"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:HeadObject"
        ]
        Resource = "${data.aws_s3_bucket.main.arn}/*"
      },
      {
        Sid    = "S3MultipartUpload"
        Effect = "Allow"
        Action = [
          "s3:AbortMultipartUpload",
          "s3:ListMultipartUploadParts"
        ]
        Resource = "${data.aws_s3_bucket.main.arn}/*"
      },
      {
        Sid    = "S3BucketOperations"
        Effect = "Allow"
        Action = [
          "s3:ListBucketMultipartUploads"
        ]
        Resource = data.aws_s3_bucket.main.arn
      }
    ]
  })
}

# IAM policy for CloudWatch Logs
resource "aws_iam_role_policy" "backend_cloudwatch" {
  name = "${var.project_name}-cloudwatch-access"
  role = aws_iam_role.backend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "arn:aws:logs:${var.aws_region}:${local.account_id}:log-group:/aws/ec2/${var.project_name}-backend*"
      },
      {
        Sid    = "CloudWatchMetrics"
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "cloudwatch:namespace" = "${var.project_name}/Backend"
          }
        }
      }
    ]
  })
}

# Instance profile for EC2
resource "aws_iam_instance_profile" "backend" {
  name = "${var.project_name}-backend-instance-profile"
  role = aws_iam_role.backend.name
}

