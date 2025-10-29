# Sword Combat Plugin
### Expanded Minecraft Combat Capabilities: Throwing Swords, Entities, and Non-Consumable Items


## To test this plugin
**(Assuming you are using a Windows Machine. Otherwise, some steps and commands will be different)**

1 - Download a paper server jar (for minecraft 1.20.8) from https://papermc.io/downloads/paper

2 - Double click the jar - this will start the setup for your local server

3 - find 'eula.txt' in the same directory as the jar file. In the eula.txt, change 'eula=false' to 'eula=true'

4 - Clone the repo

5 - Open the project in IntelliJ > File > Project Strucuture > Artifacts > + > JAR > From Modules with Dependencies > OK

6 - Add the paper-plugin.yml to the output path of the jar by clicking the + (above the .jar output), choose File, and then navigate to the paper-plugin.yml and click Open

7 - Click Apply, and then go to Build > Build Artifacts > Press Enter

8 - In your 'out' directory, go to artifacts, and copy the absolute output path of the .jar file.

9 - With the output path of your plugin in your clipboard, create a start.txt file in your paper server directory, open it, and paste the following:
  ```
  copy "*Paste your clipboard here*" "*Path to your server directory*\plugins\Sword.jar"
  
  @echo off
  
  java -Xms4096M -Xmx4096M -jar paper-1.21.8-60.jar nogui
  
  pause
  ```
  Then save the start.txt as start.bat 

10 - Run the start.bat file (you may nav to the server folder and execute the command ```./start.bat```)

11 - Join the Server after launching minecraft by either 

  - Using Direct Connect with address ```0```
  - Adding a new server, also with address ```0``` and selecting that server


The in game controls are currently:
| **Action**                    | **Input**                   | **Description / Notes**                                                                                               |
| ----------------------------- | --------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **Throw Item**                | `Drop → Right → Hold Right` | Throws a non-consumable or interactable item.                                                                         |
| **Attack**                    | `Left → Left → Left`        | Executes a combo of three consecutive attacks.                                                                        |
| **Grab Lodged Sword / Enemy** | `Shift → Left`              | Pulls a lodged sword from the ground or grabs an entity.                                                              |
| **Throw a grabbed entity**    | `Drop`                      | Hurls the grabbed entity.                                                                                             |
| **Dash Forward**              | `Swap Item → Swap Item`     | Performs a quick forward dash.                                                                                        |
| **Dash Backward**             | `Shift → Shift`             | Performs a quick backward dash. <br> Dashing while targeting a lodged sword causes you to dash to it and pull it out. |


Enjoy testing this combat system!

## Current Work

This is a work in progress, with many features & ideas yet to be implemented. 

I encourage collaboration, tips, ideas, or code improvements, you may contact me through discord: https://discord.gg/n5vty6m7
