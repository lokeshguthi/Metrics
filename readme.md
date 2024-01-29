Development
===========

User documentation
------------------

Start the development server with:

    docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material

Access the live preview on http://localhost:8000/ and change the contents inside the `/docs/` folder and `/mkdocs.yml`.

Documentation: [mkdocs](https://www.mkdocs.org/)


Java Code
---------

Requirements:

 - Java
 - Gradle

Install the dependency locally:

1. `git submodule init && git submodule update` or directly do `git clone --recursive` when cloning this repository
2. `cd dependencies/Optimus`
3. `mvn package`

To access the user documentation from within Exclaim, you first need to build the static html files to the `_docs` folder:

    docker run --rm -it -v ${PWD}:/docs -v ${PWD}/_docs:/site squidfunk/mkdocs-material build -d /site

Run a local instance of Exclaim with:

    gradle bootRun



Build the project with:

    gradle assemble




