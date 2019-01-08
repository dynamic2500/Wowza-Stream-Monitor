# Wowza-Stream-Monitor
Module to help monitor and get bitrate of Stream (RTMP) pushed to Wowza

## Manual Guide

### Build
#### Fresh install
- Install Wowza by follow link: https://www.wowza.com/pricing/installer (Prefer)
- Download project source code
- Import to Eclipse
- Build project
- The file after build will be in build folder

#### Fast install
- If you don't have install key or just need fast build, 
  - Download wowzalib.tar in this project
  - Untar to any location
  - Download project source code
  - Import to Eclipse
  - configure Project Path point to untar folder
  - Build project 
  - The file after build will be in build folder 

### Install Module
- Copy module file after build (WowzaModuleStreamMonitor.jar) to **\<Wowza install Path\>/lib**
- Restart Wowza

### Configure Application
- The Module like as another Wowza Application Module, just apply in per Application.
- Define Application using Module by follow:
  - Insert this code to Application.xml file of which Application you want to enable monitor ingest stream
    - To Application Modules Tag
  ```xml
  
    <Module>
      <Name>Monitor Stream</Name>
      <Description></Description>
      <Class>com.dynamic2500.ModuleStreamMonitor</Class>
    </Module>
  
  ```
----------------------------------
    - To Application Properties Tag (optional). The below defined is default
  ```xml

    <Property>
      <!-- Delay interval between 2 time of checking signal (ms) -->
      <Name>monitorInterval</Name>
      <Value>30000</Value>
      <Type>Integer</Type>
    </Property>
    <Property>
      <!-- the Path to save check result WowzaPath/monitorPath -->
      <Name>monitorPath</Name>
      <Value>livemonitor</Value>
      <Type>String</Type>
    </Property>

  ```
- Restart Application (or restart Wowza)

### How to know the Stream live or not
- Content in the monitor file will be changed every interval time reached. So if the modified time result file does not change, the stream is really died
- If the modified time of result file is still grow, you read the content of file and parse by format <status>:<bitrate>. If status = 0, the stream is really died
- If the stream is ok, the bitrate is the real bitrate data of stream 

### Testing
- Push a RTMP channel to Wowza Application which you installed and configured ModuleStreamMonitor
- The monitor result will be created to: **\<WowzaPath>/\<monitorPath>/\<ApplicationName>/streamName**
- The result is one line: **\<status>:\<bitrate>**
  - status: 0 or 1
  - bitrate: content bitrate of stream
-Example:
  - Assumption we had configured *live* application and push the stream with name is stream1
  - Using All default Propertise
  - We will have the result file in : /usr/local/WowzaStreamingEngine/livemonitor/live/stream1
