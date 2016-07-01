# nifi-opencv

## Installation
nifi-opencv depends on the OpenCV Java bindings. In order to make these native bindings available on the nifi instance
you must first copy the native binding for your OS from lib/native/osx/libopencv_java300.dylib (currently only OSX supported)
to your JAVA_HOME/jre/lib directory. Ensure that this is the same instance of Java that your nifi instance will be 
using.

To build nifi-opencv simply run ```mvn clean install``` from the project root directory. Once the build has completed
you can copy the /nifi-opencv-nar/target/nifi-opencv-nar-1.0-SNAPSHOT.nar to your NIFI_HOME/lib directory and restart.
Upon NiFi restart a new processor named "ObjectDetectionProcessor" will be present for your use.
 
## Detect Faces and Eyes Screenshot
This flow can be found in gitresources/ObjectDetection-FacialRecognition-V5.xml:  
![Detect Faces and Eyes](https://github.com/jdye64/nifi-opencv/blob/master/gitresources/ObjectDetection.png "ObjectDetectionProcessor")
 
## ObjectDetectionProcessor JSON Format
The processor works by detecting a parent image (a face for example) and then detecting its children images (eyes within a face for example).
Each "opencv_xml_cascade_path" is a path to a local system resource that defines the Object that is desired to be detected. This example
uses faces and eyes but this certainly doesn't have to be limited to that but rather can be any opencv model or a custom trained
opencv model. For convenience purposes the models listed below have been included in this project at nifi-opencv-processors/lib/opencv_models

```
{
  "DetectionDefinition": [
    {
      "name": "face",
      "drawBounds": "false",
      "crop": "true",
      "opencv_xml_cascade_path": "/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/haarcascades/haarcascade_frontalface_default.xml",
      "children": [
        {
          "name": "left eye",
          "drawBounds": "true",
          "crop": "false",
          "opencv_xml_cascade_path": "/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/haarcascades/haarcascade_lefteye_2splits.xml"
        },
        {
          "name": "right eye",
          "drawBounds": "true",
          "crop": "true",
          "opencv_xml_cascade_path": "/usr/local/Cellar/opencv3/3.0.0/share/OpenCV/haarcascades/haarcascade_righteye_2splits.xml"
        }
      ]
    }
  ]
}
```