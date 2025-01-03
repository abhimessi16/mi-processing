# Misinformation Investigate - Processing pipeline - Flink

The stream processing pipeline using Apache Flink.

Requirements

    Java 11
    Maven

Clone the repo - https://github.com/abhimessi16/mi-processing

    cd into repo folder
    Create application.properties using example.properties file
    Enter all the relevant details in properties file
    If running using jar file then make sure flink dependencies are built.
        Follow - https://nightlies.apache.org/flink/flink-docs-release-1.4/start/building.html
        Run - mvn clean install
        Run the jar file
    If running as a project
        make sure all dependencies are built
        Run DataStreamJob's main method
