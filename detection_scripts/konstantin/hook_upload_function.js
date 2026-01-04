//detects when function uploadImage gets called
Java.perform(function () {
    var Fragment = Java.use(
        "de.foo.bar.VeryTrustfulMNO.WifiScanner.AddProfileInfoFragment"
    );

    Fragment.uploadImage.implementation = function (imageFile, firstName, lastName, callback) {
        console.log("[*] uploadImage called");
        console.log("firstName =", firstName);
        console.log("lastName  =", lastName);
        console.log("file      =", imageFile.getAbsolutePath());

        return this.uploadImage(imageFile, firstName, lastName, callback);
    };
});

//checks if u 
Java.perform(function () {
    var RequestBuilder = Java.use("okhttp3.Request$Builder");

    RequestBuilder.build.implementation = function () {
        var request = this.build();

        var url = request.url().toString();
        var method = request.method();

        if (method === "POST") {
            console.log("\n[!] UPLOAD DETECTED");
            console.log("    -> URL: " + url);
            console.log("    -> Host: " + request.url().host());
        }

        return request;
    };
});