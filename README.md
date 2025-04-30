
# LatentAI LRE SDK Android Sample Application

This LatentAI LRE SDK sample application is demonstrating how to use `LatentAI LRE SDK` to develop an Android application running
an AI Model to accomplish your tasks.
This app allows us to get images from Camera, pass them to a running model, run inference to get results, 
create visualize components such as bounding boxes and label overlays.

This example is loosely based on [Google CodeLabs - Getting Started with CameraX](https://codelabs.developers.google.com/codelabs/camerax-getting-started)

# Build Instructions
## Requirements
- Android SDK 30+
- Android NDK r21+

## Dependencies
There are only a couple of dependencies for building this application, with all of them taken care
for you if you import this project into Android Studio.

## Downloading LRE SDK
Download SDK AAR from [here](https://repository.latentai.com/repository/files/android/1.0.0/android-lre-release-1.0.0.aar)
Copy the `android-lre-release-1.0.0.aar` to `app/libs`

## Providing Models
Download a model from `LatentAI NEXUS `
Add a model to the application, you need to place it inside `app/src/main/assets/models`. 
Models are stored in directories which looks something like the following:

```
app/src/main/assets/models/
└── the_name_of_my_model
    ├── deploy_labels.txt
    ├── modelLibrary.so
    └── deploy_manifest.json

```
The above is an example for a classifier model, which uses the classifier pre/post processors as
defined by `...processor.DetectorPreprocessor` and `...processor.DetectorMnetNMSPostprocessor` in deploy_manifest.json.
This manifest is likely to change over time, and should eventually be replaced by the LEIP model schema file. 

## Overriding Pre/Post processors
When you downloading a new model from `LatentAI Repository`, the deploy_manifest.json `output_ctx` will describe
details of expected input data for preprocessor and output data for postprocessor.
Please follow this sample app implemented `DetectorPreprocessor` and `DetectorMnetNMSPostprocessor`
to implement your model specific preprocessor and postprocessor.
For example:
```
class DetectorPreprocessor(model: Model?) : InferPreprocessor(model) {
    override fun <T> preprocess(data: T, dataType: DataType): T {
        ... ...
    }
```
```
class DetectorMnetNMSPostprocessor(model: Model?) : InferPostprocessor(model) {
    override fun <T : Any?> run(
        options: InferenceOptions?
    ): List<T?>? {
        ... ...
    }
```

<img width=40% src="images/screenshot1.jpg" alt="App Screenshot" />