const submitButton = document.getElementById('submitButton');
const imageData = document.getElementById('imageData');
const imageEl = document.getElementById('sampleImage');

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

async function infer(imageData) {
  var img = new Image(227, 227);

  img.onload = async function() {
    const image = dl.Array3D.fromPixels(img);
    const inferenceResult = await squeezeNet.predict(image);
    await inferenceResult.logits.data();

    const topClassesToProbs = await squeezeNet.getTopKClasses(inferenceResult.logits, 10);

    var result = {};

    for (const className in topClassesToProbs) {
      result[className] = topClassesToProbs[className].toFixed(5);
    }

    DeepLearnJS.reportResult(JSON.stringify(result));
  }

  img.src = 'data:image/png;base64,' + imageData;
  sampleImage.src = img.src;
  return 'ok';
}

function onSubmit() {
  infer(imageData.value);
}
