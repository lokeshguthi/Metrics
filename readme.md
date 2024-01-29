Development
===========

User documentation
------------------

Start the development server with:

    docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material

Access the live preview on http://localhost:8000/ and change the contents inside the `/docs/` folder and `/mkdocs.yml`.

Documentation: [mkdocs](https://www.mkdocs.org/) / [mkdocs-material](https://squidfunk.github.io/mkdocs-material/)


Java Code
---------

Requirements:

 - Java
 - Gradle

Install the [Optimus](https://softech-git.informatik.uni-kl.de/ag/Optimus) dependency locally:

1. `git submodule init && git submodule update` or directly do `git clone --recursive` when cloning this repository
2. `cd dependencies/Optimus`
3. `mvn package`

To access the user documentation from within Exclaim, you first need to build the static html files to the `_docs` folder:

    docker run --rm -it -v ${PWD}:/docs -v ${PWD}/_docs:/site squidfunk/mkdocs-material build -d /site

Run a local instance of Exclaim with:

    gradle bootRun

Optionally, you can continously build the application in parallel:

    gradle build -x test --continuous

- URL: http://localhost:3000/exclaim
  - Port: Configuration entry `server.port`
  - Path: Configuration entry `server.servlet.context-path`
- Username: `admin`
  - hard-coded in `src/main/java/de/tukl/softech/exclaim/security/ExclaimAuthenticationProvider.java` method `authenticate`
- Password: `softex`
  - Configuration entry `exclaim.admin.pw`

Build the project with:

    gradle assemble


Deploy
======

From within GitLab
------------------

1. Update `master` and wait for the `build` jobs to succeed (green pipeline)
2. In the pipeline click the play button for deployment
3. Check that the deploy job succeeds
4. If problems occur you can go to Operations -> Environments -> production and rollback to a previous deployment


Manually
--------

Build the documentation with:

    docker run --rm -it -v ${PWD}:/docs -v ${PWD}/_docs:/site squidfunk/mkdocs-material build -d /site

Build the project with:

    gradle assemble

The jar with dependencies is now in `build/libs/exclaim-java-0.0.1-SNAPSHOT.jar`

Copy it to the server:

    scp build/libs/exclaim-java-0.0.1-SNAPSHOT.jar softech-up:~/

Then connect to server and replace old jar:

    ssh softech-up
    cd /opt/exclaim/
    cp exclaim.jar ~/exclaim-backup.jar
    sudo cp ~/exclaim-java-0.0.1-SNAPSHOT.jar exclaim.jar
    sudo systemctl restart exclaim
    
### SSH Config
    
 For deployment it makes sense to setup an alias in `~/.ssh/config`, for example:
 
     Host softech-up softech-up.informatik.uni-kl.de
       Hostname softech-up.informatik.uni-kl.de
       User zeller


Configuration
=============

- Default values in `src/main/resources/application.properties`
- Overrides in `application.properties` (in root folder / next to the `exclaim.jar` file)


Migration from Stats to Exclaim
===============================

See https://softech-git.informatik.uni-kl.de/stats/transfer/tree/exclaim
