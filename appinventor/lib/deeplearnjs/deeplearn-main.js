"use strict";

const submitButton = document.getElementById('submitButton');

const math = new dl.NDArrayMathGPU();
// squeezenet is loaded from https://unpkg.com/deeplearn-squeezenet
const squeezeNet = new squeezenet.SqueezeNet(math);

async function lol() {
  await squeezeNet.load();

  console.log("DeepLearnJS: deeplearn-main.js load");
  DeepLearnJS.ready();
}

console.log("DeepLearnJS: deeplearn-main.js start");
lol();

var video = document.createElement('video');
video.setAttribute('autoplay', '');
video.setAttribute('playsinline', '');
video.width = 500;
video.style.display = 'none';

var frontFacing = true;
var videoConstraints = {video: { facingMode: frontFacing ? "user" : "environment" }, audio: false};
var isPlaying = false;
var isVideoMode = false;

var img = new Image();
img.width = 500;

var isImageShowing = true;
img.style.display = 'block';

document.body.appendChild(video);
document.body.appendChild(img);

video.addEventListener( "loadedmetadata", function (e) {
    video.height = this.videoHeight * video.width / this.videoWidth;
}, false );

function start() {
  if (!isPlaying && isVideoMode) {
    navigator.mediaDevices.getUserMedia({video: { facingMode: frontFacing ? "user" : "environment" }, audio: false})
    .then(stream => (video.srcObject = stream))
    .catch(e => log(e));
    isPlaying = true;
    video.style.display = 'block';
  }
}

function stop() {
  if (isPlaying && isVideoMode && video.srcObject) {
    video.srcObject.getTracks().forEach(t => t.stop());
    isPlaying = false;
    video.style.display = 'none';
  }
}

function toggleCameraFacingMode() {
  frontFacing = !frontFacing;
  stop();
  start();
}

async function infer(imageData) {
  console.log("DeepLearnJS: in infer");

  console.log("DeepLearnJS: onload");
  const image = dl.Array3D.fromPixels(imageData);
  const resized = dl.image.resizeBilinear(image, [227, 227]);
  console.log("DeepLearnJS: fromPixels");
  const logits = squeezeNet.predict(resized);
  console.log("DeepLearnJS: squeezeNet predict");

  const topClassesToProbs = await squeezeNet.getTopKClasses(logits, 10);
  
  console.log("DeepLearnJS: squeezeNet getTopK");

  var result = [];

  for (const className in topClassesToProbs) {
    result.push([className, topClassesToProbs[className].toFixed(5)]);
  }
    
  console.log("DeepLearnJS: JSON stringify is " + JSON.stringify(result));

  DeepLearnJS.reportResult(JSON.stringify(result));
    
  console.log("DeepLearnJS: reportResult called");

  console.log("DeepLearnJS: end infer");
  return 'ok';
}

function classifyVideoData() {
  if (isPlaying && isVideoMode) {
    infer(video);    
  }
}

function classifyImageData(imageData) {
  if (!isVideoMode) {
    img.onload = function() {
      infer(img);
    }
    img.src = 'data:image/png;base64,' + imageData;
  }
}

function showImage() {
  if (!isImageShowing && !isVideoMode) {
    img.style.display = 'block';
    isImageShowing = true;
  }
}

function hideImage() {
  if (isImageShowing) {
    img.style.display = 'none';
    isImageShowing = false;
  }
}

function setInputMode(inputMode) {
  if (inputMode == 'image' && isVideoMode) {
    stop();
    isVideoMode = false;
    showImage();
  } else if (inputMode == 'video' && !isVideoMode) {
    hideImage();
    isVideoMode = true;
    start();
  }
}

function setInputWidth(width) {
  video.width = width;
  video.height = video.videoHeight * width / video.videoWidth;
  img.width = width;
}
