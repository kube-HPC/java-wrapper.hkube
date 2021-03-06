Create a HKube java written algorithm.

1. Create your development project.
   a) Access hkube artifacts.

     Your java maven project will depend on 2 hkube artifacts:
     * io.hkube:java-algo-parent:2.0-SNAPSHOT:pom
     * io.hkube:interfaces:2.0-SNAPSHOT:jar

     hkube artifacts are deployed on maven remote repository.
     Access them by adding "-DremoteRepositories=https://oss.sonatype.org/content/repositories/snapshots"
     to any maven command.



   b) No access to the internet
     If you have no access to the internet in your dev environment, you'll have to download the artifacts from the internet first.

     run:
     mvn dependency:get -Dartifact=io.hkube:java-algo-parent:2.0-SNAPSHOT:pom -DremoteRepositories=https://oss.sonatype.org/content/repositories/snapshots -Ddest=java-algo-parent.xml
     mvn dependency:get -Dartifact=io.hkube:interfaces:2.0-SNAPSHOT:jar -DremoteRepositories=https://oss.sonatype.org/content/repositories/snapshots -Ddest=interfaces.jar

     Then install them in your enclosed environment.

     run:
     mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file  -Dfile=./interfaces.jar -DgroupId=io.hkube -DartifactId=interfaces -Dversion=2.0-SNAPSHOT -Dpackaging=jar
     mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file  -Dfile=./java-algo-parent.xml -DgroupId=io.hkube -DartifactId=java-algo-parent -Dversion=2.0-SNAPSHOT -Dpackaging=pom

   c) pom.xml.
        * Create a java maven project, have the projects pom.xml inherit from io.hkube:java-algo-parent:2.0-SNAPSHOT:pom
             <parent>
                   <artifactId>java-algo-parent</artifactId>
                   <groupId>io.hkube</groupId>
                   <version>2.0-SNAPSHOT</version>
              </parent>

        * Make sure any dependency your project depends on in runtime, is included in pom.xml dependencies list.

    d) Access to private repository
        The sources of the projects you create , will later be deployed to hkube. "maven package" command will be ran on your project inside hkubes kubernetis cluster. You can overwrite the settings.xml used by hkubes maven, by placeing your own settings.xml under the projects maven resource folder.
        kubernetis Secrets values of maven_user and maven_token, can be accessed in the settings file as follows:

           <servers>
                <server>
                    <id>private_repo</id>
                    <username>${PACKAGES_REGISTRY_USER}</username>
                    <password>${PACKAGES_TOKEN}</password>
                </server>
            </servers>

       ( maven_user -> PACKAGES_REGISTRY_USER
        maven_token -> PACKAGES_TOKEN )

    d) Algorithm implementation
     Write a class implementing hkube.algo.wrapper.IAlgorithm that will invoke your code algorithm from "start" its method.
     This class must be included in your project source or as part of one of the projects dependencies.



2. Deploy your artifact to hkube.

    A new algorithm can be created and registered from code via hkube UI or rest api.
    1. If created from a git repository - make sure the repository root is the root of the java maven project created as described in 1.c.
    2. If created from an archived project - you can use maven assembly plugin to create the archive, as used in algorithm-example

    When deploying the algorithm, attributes are set in the New Algorithm ui form, or the rest request payload.
    Make sure the following attributes are set as follows:
    1. "env" to "java"
    2. "entryPoint" to the fully qualified name of your class implementing IAlgorithm.

3. Run algorithm locally (for debugging) and as part of a pipeline on the cluster.

    a. Main class.
       Java main class is hkube.algo.wrapper.Main from artifact io.hkube:wrapper:2.0-SNAPSHOT (also found in https://oss.sonatype.org/content/repositories/snapshots)
    
    b. Runtime ClassPath
    
       1) io.hkube:wrapper:2.0-SNAPSHOT and its dependencies.
       
       2) The written algorithm module.
    
    c. Environment variables:
       When running the algorithm locally you need to set 2 environment variables:

      1) ALGORITHM_ENTRY_POINT - The name of the class you wrote implementing IAlgorithm.
      
      2) WORKER_SOCKET_URL - path obtained from a the debug algorithm defined on hkube deployment. (example ws://63.34.172.241/hkube/debug/something)
      
      Instead of setting these environment variables, you can also add a config.properties file to the running classpath root directory and set these environment variable names as keys.
       
                WORKER_SOCKET_URL=ws://63.34.172.241/hkube/debug/something
                ALGORITHM_ENTRY_POINT=Algorithm
                
    d. Program argument 'debug'
        To avoid the program attempting to load the algorithm from a jar, 'debug' should be given as a program argument.
