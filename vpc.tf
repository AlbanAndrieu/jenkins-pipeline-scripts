module "vpc" {
  source             = "terraform-aws-modules/vpc/aws"
  name               = "java_vpc"
  cidr               = "10.0.0.0/16"
  azs                = ["eu-central-1a", "eu-central-1b", "eu-central-1c"]
  private_subnets    = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  enable_nat_gateway = true
  enable_vpn_gateway = true
  tags = {
    Terraform   = "true"
    Environment = "dev"
  }
  public_subnet_tags = {
    "kubernetes.io/cluster/java-cluster" = "shared"
    "kubernetes.io/role/elb"             = 1
  }
  private_subnet_tags = {
    "kubernetes.io/cluster/java-cluster" = "shared"
    "kubernetes.io/role/internal-elb"    = "1"
  }
}
