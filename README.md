## Installation for Linux
1. If you do not already have a server directory, follow these instructions, if you already do skip to step 2.
    1.  Open your Minecraft launcher
    2. Select or create an installation of version 1.21.8.
    3. Within the edit screen, above the "Version" section you should see a download option for your server, download it, and create a new directory where you want to store your server directory. I recommend naming it the version itself (i.e. "1.21.8") for good organization.
    4. Place `server.jar` within this new directory
    5. Open this directory in your terminal, and run `java -jar server.jar`
    6. Edit `eula.txt` and edit `eula=false` to `eula=true`
    7. Rerun `java -jar server.jar`


2. Download a paper server jar (for Minecraft 1.21.8) from https://papermc.io/downloads/paper, and put it in your server directory.


3.  Install and/or update java, at least at version OpenJDK 21. Some systems may be defaulted to older versions of java, so ensure that you successfully set it to the appropriate version before continuing


4. Run `java -jar {downloaded paper file}`


5. Install IntelliJ onto your system if you do not already have it


6. Inside of IntelliJ, clone the repository at this link: https://github.com/Mere-Solace/Sword-Combat-Plugin


7. After cloning, in IntelliJ, navigate: File > Project Structure > Artifacts > + > JAR > From Modules with Dependencies > OK


8. Select '+' above the .jar's output section, and click 'File', now navigate inside of the repo to 'Sword-Combat-Plugin/src/resources/paper-plugin.yml' and press 'OK'.


9. Click apply


10. Now click the icon with three bars, navigate to Build > Build Artifacts, and then press enter.


11. Inside of IntelliJ, look inside of the Sword-Combat-Plugin's directory. Inside of this directory navigate to `out/artifacts/sword_jar`. Right click sword.jar and select Copy Path/Reference, and select Absolute Path


12. Enter a terminal inside of your server's directory and do:
    1. `touch start.bash`
    2. Enter `start.bash` in a text editor, Vim is recommended but anything works.
```
#!/bin/bash
cp {paste-here} {path to your server directory}/plugins/sword.jar
java -Xms4096M -Xmx4096M -jar paper-1.21.8-60.jar --nogui
```


13. Go ahead and make it executable by running `chmod +x start.bash`


14. You can now initiate your server with the sword plugin at any time by running `./start.bash`, which can be connected to by direct connecting to an address of `0` 
