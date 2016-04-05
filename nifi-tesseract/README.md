# NiFi-Tesseract

## Installation
Running nifi-tesseract requires that nifi-tesseract-nar-0.5.1.nar (you must build this with ```mvn clean install package```)
be installed in your $NIFI_HOME/lib directory and that you have installed the required system dependencies for Tesseract. 
Until I get the time this is currently only supported on OS X through running this brew installation command
```brew install tesseract --all-languages --with-training-tools``` Once you have satisfied these dependencies you should
be able to start NiFi as usual and use the "TesseractOCR" processor.

## Sample Template
The "TesseractOCR" processor expects an image in any format be present in the incoming FlowFiles content. A sample workflow
screenshot has been included below.

![NiFiTesseractScreenshot](/nifi-tesseract/assets/Tesseract_QuickBrownFox_Screenshot.png)


An example template can be found in ```$NIFI_TESSERACT_HOME/templates/Tesseract_QuickBrownFox_*.xml```

## Docker
This project Docker Image is available in DockerHub and can be ran using ```docker run -d -p 8080:8080 jdye64/nifi-tesseract:1.5``` at this point the NiFi instance can be accessed by navigating your browser to http://{YOUR_DOCKER_INSTANCE_IP}:8080
