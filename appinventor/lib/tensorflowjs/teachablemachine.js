'use strict';

console.log('TeachableMachine: Using Tensorflow.js version ' + tf.version.tfjs);

//const MOBILENET_MODEL_PATH = 'https://storage.googleapis.com/tfjs-models/tfjs/mobilenet_v1_0.25_224/model.json';
const MOBILENET_MODEL_PATH = 'model.json';

const NUM_CLASSES = 3;
const IMAGE_SIZE = 224;
const TOPK = 10;

class KNNImageClassifier {
  constructor(numClasses, k) {
    this.numClasses = numClasses;
    this.k = k;
    this.classLogitsMatrices = [];
    this.classExampleCount = [];
    this.varsLoaded = false;
    this.squashLogitsDenominator = tf.scalar(300);
    for (let i = 0; i < this.numClasses; i++) {
      this.classLogitsMatrices.push(null);
      this.classExampleCount.push(0);
    }
  }

  async load() {
    this.mobilenet = await tf.loadModel(MOBILENET_MODEL_PATH);
    this.mobilenet.predict(tf.zeros([1, IMAGE_SIZE, IMAGE_SIZE, 3])).dispose();
    this.varsLoaded = true;
    console.log('TeachableMachine: KNNImageClassifier ready');
  }

  clearClass(classIndex) {
    if (classIndex >= this.numClasses) {
      console.log(`Cannot clear invalid class ${classIndex}`);
      return;
    }
    this.classLogitsMatrices[classIndex] = null;
    this.classExampleCount[classIndex] = 0;
    this._clearTrainLogitsMatrix();
  }

  addImage(image, classIndex) {
    if (!this.varsLoaded) {
      console.warn('Cannot add images until vars have been loaded.');
      return;
    }
    if (classIndex >= this.numClasses) {
      console.warn(`Cannot add to invalid class ${classIndex}`);
    }
    this._clearTrainLogitsMatrix();
    tf.tidy(() => {
      const logits = tf.tidy(() => {
        const offset = tf.scalar(127.5);
        const normalized = image.sub(offset).div(offset);
        const batched = normalized.reshape([1, IMAGE_SIZE, IMAGE_SIZE, 3]);
        return this.mobilenet.predict(batched);
      });
      const imageLogits = this._normalizeVector(logits);
      const logitsSize = imageLogits.shape[1];
      if (this.classLogitsMatrices[classIndex] == null) {
        this.classLogitsMatrices[classIndex] = imageLogits.as2D(1, logitsSize);
      } else {
        const newTrainLogitsMatrix =
            this.classLogitsMatrices[classIndex]
                .as2D(this.classExampleCount[classIndex], logitsSize)
                .concat(imageLogits.as2D(1, logitsSize), 0);
        this.classLogitsMatrices[classIndex].dispose();
        this.classLogitsMatrices[classIndex] = newTrainLogitsMatrix;
      }
      tf.keep(this.classLogitsMatrices[classIndex]);
      this.classExampleCount[classIndex]++;
    });
  }

  predict(image) {
    if (!this.varsLoaded) {
      throw new Error('Cannot predict until vars have been loaded.');
    }
    return tf.tidy(() => {
      const logits = tf.tidy(() => {
        const offset = tf.scalar(127.5);
        const normalized = image.sub(offset).div(offset);
        const batched = normalized.reshape([1, IMAGE_SIZE, IMAGE_SIZE, 3]);
        return this.mobilenet.predict(batched);
      });
      const imageLogits = this._normalizeVector(logits);
      const logitsSize = imageLogits.shape[1];
      if (this.trainLogitsMatrix == null) {
        let newTrainLogitsMatrix = null;
        for (let i = 0; i < this.numClasses; i++) {
          newTrainLogitsMatrix = this._concatWithNulls(
              newTrainLogitsMatrix, this.classLogitsMatrices[i]);
        }
        this.trainLogitsMatrix = newTrainLogitsMatrix;
      }
      if (this.trainLogitsMatrix == null) {
        console.warn('Cannot predict without providing training images.');
        return null;
      }
      tf.keep(this.trainLogitsMatrix);
      const numExamples = this._getNumExamples();
      return this.trainLogitsMatrix.as2D(numExamples, logitsSize)
          .matMul(imageLogits.as2D(logitsSize, 1))
          .as1D();
    });
  }

  async predictClass(image) {
    let imageClass = -1;
    const confidences = new Array(this.numClasses);
    if (!this.varsLoaded) {
      throw new Error('Cannot predict until vars have been loaded.');
    }
    const knn = this.predict(image).asType('float32');
    const numExamples = this._getNumExamples();
    const kVal = Math.min(this.k, numExamples);
    const topK = this._topK(await knn.data(), kVal);
    knn.dispose();
    const topKIndices = topK.indices;
    if (topKIndices == null) {
      return {classIndex: imageClass, confidences};
    }
    const indicesForClasses = [];
    const topKCountsForClasses = [];
    for (let i = 0; i < this.numClasses; i++) {
      topKCountsForClasses.push(0);
      let num = this.classExampleCount[i];
      if (i > 0) {
        num += indicesForClasses[i - 1];
      }
      indicesForClasses.push(num);
    }
    for (let i = 0; i < topKIndices.length; i++) {
      for (let classForEntry = 0; classForEntry < indicesForClasses.length;
           classForEntry++) {
        if (topKIndices[i] < indicesForClasses[classForEntry]) {
          topKCountsForClasses[classForEntry]++;
          break;
        }
      }
    }
    let topConfidence = 0;
    for (let i = 0; i < this.numClasses; i++) {
      const probability = topKCountsForClasses[i] / kVal;
      if (probability > topConfidence) {
        topConfidence = probability;
        imageClass = i;
      }
      confidences[i] = probability;
    }
    return {classIndex: imageClass, confidences};
  }

  _topK(values, k) {
    const valuesAndIndices = [];
    for (let i = 0; i < values.length; i++) {
      valuesAndIndices.push({value: values[i], index: i});
    }
    valuesAndIndices.sort((a, b) => {
      return b.value - a.value;
    });
    const topkValues = new Float32Array(k);
    const topkIndices = new Int32Array(k);
    for (let i = 0; i < k; i++) {
      topkValues[i] = valuesAndIndices[i].value;
      topkIndices[i] = valuesAndIndices[i].index;
    }
    return {values: topkValues, indices: topkIndices};
  }

  getClassExampleCount() {
    return this.classExampleCount;
  }

  getClassLogitsMatrices() {
    return this.classLogitsMatrices;
  }

  setClassLogitsMatrices(classLogitsMatrices) {
    this.classLogitsMatrices = classLogitsMatrices;
    this.classExampleCount = classLogitsMatrices.map(
        tensor => tensor != null ? tensor.shape[0] : 0);
    this._clearTrainLogitsMatrix();
  }

  _clearTrainLogitsMatrix() {
    if (this.trainLogitsMatrix != null) {
      this.trainLogitsMatrix.dispose();
      this.trainLogitsMatrix = null;
    }
  }

  _concatWithNulls(ndarray1, ndarray2) {
    if (ndarray1 == null && ndarray2 == null) {
      return null;
    }
    if (ndarray1 == null) {
      return ndarray2.clone();
    } else if (ndarray2 === null) {
      return ndarray1.clone();
    }
    return ndarray1.concat(ndarray2, 0);
  }

  _normalizeVector(vec) {
    const squashedVec = tf.div(vec, this.squashLogitsDenominator);
    const sqrtSum = squashedVec.square().sum().sqrt();
    return tf.div(squashedVec, sqrtSum);
  }

  _getNumExamples() {
    let total = 0;
    for (let i = 0; i < this.classExampleCount.length; i++) {
      total += this.classExampleCount[i];
    }
    return total;
  }

  dispose() {
    this.mobilenet.dispose();
    this._clearTrainLogitsMatrix();
    this.classLogitsMatrices.forEach(
        classLogitsMatrix => classLogitsMatrix.dispose());
    this.squashLogitsDenominator.dispose();
  }
}

var training = -1;
var videoPlaying = false;

var knn = new KNNImageClassifier(NUM_CLASSES, TOPK);

var video = document.createElement('video');
video.setAttribute('autoplay', '');
video.setAttribute('playsinline', '');
video.width = 500;
video.style.display = 'block';

var frontFacing = true;

document.body.appendChild(video);

var timer;

var labelToClass = {};
var classToLabel = {};

var confidences = {};

var topChoice;

var availableClasses = [];

for (let i = 0; i < NUM_CLASSES; i++) {
  availableClasses.push(i);
}

video.addEventListener('loadedmetadata', function () {
  video.height = this.videoHeight * video.width / this.videoWidth;
}, false);

function startVideo() {
  navigator.mediaDevices.getUserMedia({video: {facingMode: frontFacing ? 'user' : 'environment'}, audio: false})
  .then(stream => {
    video.srcObject = stream;
    video.addEventListener('playing', () => videoPlaying = true);
    video.addEventListener('paused', () => videoPlaying = false);
  }).catch(e => log(e));
}

function stopVideo() {
  if (video.srcObject) {
    video.srcObject.getTracks().forEach(t => t.stop());
  }
}

function toggleCameraFacingMode() {
  frontFacing = !frontFacing;
  stopVideo();
  startVideo();
}

startVideo();

knn.load()
.then(() => {
  start();
  TeachableMachine.ready();
});

function start() {
  if (timer) {
    stop();
  }
  video.play();
  timer = requestAnimationFrame(animate);
}

function stop() {
  video.pause();
  cancelAnimationFrame(timer);
}

function listSampleCounts() {
  var sampleCounts = knn.getClassExampleCount();
  var sList = [[], []];
  for (let i = 0; i < NUM_CLASSES; i++) {
    if (classToLabel.hasOwnProperty(i)) {
      sList[0].push(classToLabel[i]);
      sList[1].push(sampleCounts[i]);
    }
  }
  return sList;
}

function listConfidences() {
  var cList = [[], []];
  for (let i = 0; i < NUM_CLASSES; i++) {
    if (classToLabel.hasOwnProperty(i)) {
      cList[0].push(classToLabel[i]);
      cList[1].push(confidences[i]);
    }
  }
  return cList;
}

function animate() {
  if(videoPlaying) {
    const image = tf.tidy(() => {
      return tf.image.resizeBilinear(tf.fromPixels(video).toFloat(), [IMAGE_SIZE, IMAGE_SIZE]);
    });
    if(training != -1) {
      knn.addImage(image, training);
      var sList = listSampleCounts();
      TeachableMachine.gotSampleCounts(JSON.stringify(sList[0]), JSON.stringify(sList[1]));
    }
    const exampleCount = knn.getClassExampleCount();
    if(Math.max(...exampleCount) > 0) {
      knn.predictClass(image)
      .then(res => {
        for(let i = 0; i < NUM_CLASSES; i++) {
          if(res.classIndex == i) {
            topChoice = i;
          }
          if(exampleCount[i] > 0) {
            confidences[i] = res.confidences[i];
          }
        }
        var cList = listConfidences();
        TeachableMachine.gotConfidences(JSON.stringify(cList[0]), JSON.stringify(cList[1]));
        TeachableMachine.gotClassification(classToLabel[topChoice]);
      })
      .then(() => image.dispose());
    } else {
      image.dispose();
    }
  }
  timer = requestAnimationFrame(animate);
}

function startTraining(label) {
  if (!labelToClass.hasOwnProperty(label)) {
    if (availableClasses.length == 0) {
      return;
    }
    var c = availableClasses.shift();
    labelToClass[label] = c;
    classToLabel[c] = label;
  }
  training = labelToClass[label];
}

function stopTraining() {
  training = -1;
}

function getSampleCount(label) {
  if (!labelToClass.hasOwnProperty(label)) {
    return -1;
  }
  var counts = knn.getClassExampleCount();
  return counts[labelToClass[label]];
}

function getConfidence(label) {
  if (!labelToClass.hasOwnProperty(label)) {
    return -1;
  }
  return confidences[labelToClass[label]];
}

function getClassification() {
  return classToLabel[topChoice];
}

function clear(label) {
  if (labelToClass.hasOwnProperty(label)) {
    if (training === labelToClass[label]) {
      stopTraining();
    }
    knn.clearClass(labelToClass[label]);
    availableClasses.push(labelToClass[label]);
    availableClasses.sort();
    delete classToLabel[labelToClass[label]];
    delete confidences[labelToClass[label]];
    delete labelToClass[label];
    var sList = listSampleCounts();
    TeachableMachine.gotSampleCounts(JSON.stringify(sList[0]), JSON.stringify(sList[1]));
    var cList = listConfidences();
    TeachableMachine.gotConfidences(JSON.stringify(cList[0]), JSON.stringify(cList[1]));
    if (classToLabel.hasOwnProperty(topChoice)) {
      TeachableMachine.gotClassification(classToLabel[topChoice]);
    } else {
      TeachableMachine.gotClassification('');
    }
  }
}

function setInputWidth(width) {
  video.width = width;
  video.height = video.videoHeight * width / video.videoWidth;
}
