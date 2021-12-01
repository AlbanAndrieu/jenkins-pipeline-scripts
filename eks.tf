data "aws_eks_cluster" "cluster" {
  name = module.eks.cluster_id
}
data "aws_eks_cluster_auth" "cluster" {
  name = module.eks.cluster_id
}
provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}
module "eks" {
  source                 = "terraform-aws-modules/eks/aws"
  version                = "17.24.0"
  cluster_name           = "java-cluster"
  cluster_version        = "1.21"
  subnets                = module.vpc.private_subnets
  vpc_id                 = module.vpc.vpc_id
  kubeconfig_output_path = "~/.kube/"
  node_groups = {
    first = {
      desired_capacity = 2
      max_capacity     = 3
      min_capacity     = 1
      instanace_type   = "t2.small"
    }
  }
}
resource "null_resource" "java" {
  depends_on = [module.eks]
  provisioner "local-exec" {
    command = "aws eks --region eu-central-1  update-kubeconfig --name $AWS_CLUSTER_NAME"
    environment = {
      AWS_CLUSTER_NAME = "java-cluster"
    }
  }
}
