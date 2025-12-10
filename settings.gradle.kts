rootProject.name = "clastor-distributed-lock"
include("domain")
include("app")
include("adapter")
include("adapter:curator")
findProject(":adapter:curator")?.name = "curator"
include("adapter:zookeeper")
findProject(":adapter:zookeeper")?.name = "zookeeper"
