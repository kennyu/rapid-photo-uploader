# RapidPhoto Infrastructure - Terraform/OpenTofu

Infrastructure as Code for deploying RapidPhoto backend to AWS.

## What This Does

- ✅ **Secures RDS**: Makes database private (not publicly accessible)
- ✅ **Creates IAM role**: For EC2 to access S3 without credentials
- ✅ **Configures security groups**: Properly isolated network access
- ✅ **Applies S3 policies**: Enforces HTTPS and encryption
- ✅ **Launches EC2**: t4g.small instance with Java 21 pre-configured
- ✅ **Sets up monitoring**: CloudWatch logs and metrics
- ✅ **Creates Elastic IP**: Stable IP address for your backend

## Prerequisites

1. **Install Terraform or OpenTofu:**
   - Terraform: https://www.terraform.io/downloads
   - OpenTofu: https://opentofu.org/docs/intro/install/

2. **AWS CLI configured:**
   ```bash
   aws configure
   ```

3. **Generate SSH key pair:**
   ```bash
   ssh-keygen -t ed25519 -f ssh_key -N ""
   ```
   This creates `ssh_key` (private) and `ssh_key.pub` (public)

## Quick Start

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Configure Variables

```bash
# Copy example config
cp terraform.tfvars.example terraform.tfvars

# Edit with your values (optional, defaults work for most)
notepad terraform.tfvars
```

### 3. Review Changes

```bash
terraform plan
```

This shows what Terraform will create/modify.

### 4. Apply Infrastructure

```bash
terraform apply
```

Type `yes` when prompted. This takes ~5 minutes.

## What Gets Created

| Resource | Type | Cost/Month |
|----------|------|------------|
| EC2 Instance | t4g.small | ~$12 |
| Elastic IP | Fixed IP | $0 (while attached) |
| EBS Volume | 20GB gp3 | ~$2 |
| IAM Role | Free | $0 |
| Security Groups | Free | $0 |
| **Total** | | **~$14** |

*Note: S3 and RDS costs are separate (already exist)*

## After Infrastructure is Ready

Terraform will output deployment commands. Example:

```bash
# Build JAR
cd backend
./mvnw clean package -DskipTests

# Upload to EC2
scp -i terraform/ssh_key backend/target/rapid-photo-uploader-*.jar ec2-user@<IP>:/tmp/app.jar

# SSH and deploy
ssh -i terraform/ssh_key ec2-user@<IP>
sudo mv /tmp/app.jar /opt/rapidphoto/rapid-photo-uploader.jar
sudo chown rapidphoto:rapidphoto /opt/rapidphoto/rapid-photo-uploader.jar
sudo systemctl start rapidphoto-backend
sudo systemctl status rapidphoto-backend

# Test
curl http://<IP>:8080/actuator/health
```

## Useful Commands

```bash
# View current infrastructure
terraform show

# View outputs (IPs, URLs, etc.)
terraform output

# View specific output
terraform output ec2_public_ip

# SSH into EC2
terraform output -raw ssh_command | bash

# Update infrastructure (after changing .tf files)
terraform apply

# Destroy everything (careful!)
terraform destroy
```

## Security Features

✅ **RDS made private** - Only accessible from within VPC  
✅ **EC2 → RDS** - Security group allows backend to database  
✅ **S3 encryption** - Server-side AES-256 enabled  
✅ **HTTPS-only S3** - Bucket policy denies HTTP  
✅ **IMDSv2** - Enhanced EC2 metadata security  
✅ **EBS encryption** - Encrypted root volume  
✅ **IAM role** - No hardcoded AWS credentials  
✅ **SSH restrictions** - Only from your IP  

## Monitoring

### CloudWatch Logs
```bash
# View application logs
aws logs tail /aws/ec2/rapidphoto-backend --follow
```

### SSH into Instance
```bash
ssh -i ssh_key ec2-user@$(terraform output -raw ec2_public_ip)

# View service status
sudo systemctl status rapidphoto-backend

# View logs
sudo journalctl -u rapidphoto-backend -f
```

## Updating Infrastructure

### To change instance type:
```hcl
# Edit terraform.tfvars
instance_type = "t4g.medium"  # Upgrade to 4GB RAM
```

```bash
terraform apply
```

### To update backend code:
Just rebuild and upload JAR, then restart service:
```bash
sudo systemctl restart rapidphoto-backend
```

## Cost Optimization

### Use Reserved Instances
After validating your setup works:
```bash
# Go to AWS Console → EC2 → Reserved Instances
# Purchase 1-year commitment for ~40% savings
# t4g.small Reserved: ~$7/month (vs $12 on-demand)
```

### Enable S3 Intelligent-Tiering
Already configured in `s3.tf` - automatically moves old files to cheaper storage.

## Troubleshooting

### RDS not accessible
```bash
# Check RDS is private
terraform output rds_publicly_accessible  # Should be "false"

# Check security group allows EC2
aws ec2 describe-security-groups --group-ids sg-04bae24dd0a44ce57
```

### EC2 can't access S3
```bash
# SSH into EC2 and test
aws s3 ls s3://rapidphoto-uploads-297721440242/

# Check IAM role is attached
aws sts get-caller-identity
```

### Application won't start
```bash
# SSH into EC2
ssh -i ssh_key ec2-user@<IP>

# Check logs
sudo journalctl -u rapidphoto-backend -n 100 --no-pager

# Check Java process
ps aux | grep java
```

## State Management

Terraform stores state in `terraform.tfstate`. This file contains sensitive information!

**For production:**
1. Use remote state (S3 backend)
2. Enable state locking (DynamoDB)
3. Never commit state files to git

Example remote backend:
```hcl
# Add to main.tf
terraform {
  backend "s3" {
    bucket = "rapidphoto-terraform-state"
    key    = "prod/terraform.tfstate"
    region = "us-east-1"
    
    # State locking
    dynamodb_table = "terraform-state-lock"
  }
}
```

## Cleanup

To destroy all infrastructure:
```bash
terraform destroy
```

**Warning:** This will:
- Terminate EC2 instance
- Release Elastic IP
- Delete security groups
- Remove IAM roles

It will NOT:
- Delete RDS (has deletion protection)
- Delete S3 bucket
- Remove S3 data

## OpenTofu Usage

OpenTofu is a drop-in replacement for Terraform:

```bash
# Install OpenTofu
# See: https://opentofu.org/docs/intro/install/

# Use 'tofu' instead of 'terraform'
tofu init
tofu plan
tofu apply
```

All commands are identical, just replace `terraform` with `tofu`.

## Support

For issues:
1. Check `terraform output` for connection details
2. Review CloudWatch logs
3. SSH into EC2 and check systemd logs
4. Verify security groups allow required traffic

---

**Cost Estimate:** ~$14/month for EC2 + $15/month for RDS + $50/month for S3/transfer = **~$79/month total**

