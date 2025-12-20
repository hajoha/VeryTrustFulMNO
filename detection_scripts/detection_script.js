Java.perform(function () {
	// installing hooks to generically detect usage of weird functions that could mean the app has malware
	// use with: 'frida -U -f de.foo.bar.VeryTrustfulMNO -l detection_script.js'
	
	// 1. Microphone capture using AudioRecord, could be extended to check for MediaRecorder but same principle
	const AudioRecord = Java.use('android.media.AudioRecord');

	AudioRecord.startRecording.overload().implementation = function () {
    		console.log("MIC CAPTURE: AudioRecord.startRecording()");
    		return AudioRecord.startRecording.overload().call(this);
  	};

      	AudioRecord.read.overload('[B', 'int', 'int').implementation = function (b, off, len) {
        	console.log("MIC CAPTURE: AudioRecord.read() len=" + len);
        	return this.read(b, off, len);
      	};

   	AudioRecord.stop.implementation = function () {
      		console.log("MIC CAPTURE: AudioRecord.stop()");
      		return this.stop();
    	};

	// 2. Suspicious file I/O: detect audio file creation and deletion
	const File = Java.use('java.io.File');

    	File.$init.overload('java.io.File', 'java.lang.String').implementation = function (parent, child) {
  		const full = (parent ? parent.getAbsolutePath() : "(null)") + "/" + child;
  		if (child && child.toLowerCase().endsWith(".wav")) {
    			console.log("FILE: suspicious path created path=" + full);
  		}
  		return File.$init.overload('java.io.File', 'java.lang.String').call(this, parent, child);
	};

	File.delete.implementation = function () {
      		const path = this.getAbsolutePath();
      		if (path && (path.toLowerCase().endsWith(".wav") || path.toLowerCase().endsWith(".mp3"))) {
        		console.log("FILE: delete() called on audio file " + path);
		}
      		return this.delete();
    	};

	const FOS = Java.use('java.io.FileOutputStream');

   	FOS.$init.overload('java.io.File').implementation = function (file) {
        	const path = file ? file.getAbsolutePath() : "(null)";
      		if (path && (path.toLowerCase().endsWith(".wav") || path.toLowerCase().endsWith(".mp3"))) {
        		console.log("FILE WRITE: opened FileOutputStream for audio file " + path);
      		}
      		return this.$init(file);
    	};

	FOS.write.overload('[B', 'int', 'int').implementation = function (b, off, len) {
        	console.log("FILE WRITE: len=" + len);
        	return this.write(b, off, len);
     	};

	// 3) Network exfiltration: OkHttp (common in Android malware)
	const RequestBuilder = Java.use('okhttp3.Request$Builder');
	
	RequestBuilder.url.overload('java.lang.String').implementation = function (url) {
                console.log("NETWORK: OkHttp suspicious request URL url=" + url);
                return this.url(url);
     	};

	const MultipartBuilder = Java.use('okhttp3.MultipartBody$Builder');

	MultipartBuilder.addFormDataPart.overload('java.lang.String', 'java.lang.String', 'okhttp3.RequestBody').implementation = function (type, filename, body) {
          	console.log("NETWORK: suspicious upload added of type=" + type + " with name filename=" + filename);
          	return this.addFormDataPart(type, filename, body);
        };

	const OkHttpClient = Java.use('okhttp3.OkHttpClient');

    	OkHttpClient.newCall.implementation = function (req) {
      		console.log("NETWORK: OkHttpClient.newCall() -> suspicious HTTP request about to be executed");
      		return this.newCall(req);
    	};
   });
