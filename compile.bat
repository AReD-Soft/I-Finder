@echo off
echo "Compile resource class"
javac -d bin -sourcepath src src/SearchXMLWithGUI.java
echo "Compile .jar file"
jar cvfm I-Finder.jar MANIFEST.MF -C bin . -C res .
echo "Done"
