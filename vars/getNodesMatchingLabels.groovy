// Will return long list of all individual nodes with names matching label
def call(labels) {
  def nodeNames = []
  labels.each { label ->
    nodeNames += Jenkins.instance.getLabel(label).getNodes().collect { node ->
      node.getNodeName()
    }
  }
  return nodeNames.unique()
}
