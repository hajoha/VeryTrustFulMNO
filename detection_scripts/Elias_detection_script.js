Java.perform(function () {
    
    var appPackageName = "Unknown";
    
    // --- 0. DYNAMIC CONTEXT RESOLUTION ---
    try {
        const ActivityThread = Java.use("android.app.ActivityThread");
        const context = ActivityThread.currentApplication();
        if (context !== null) {
            appPackageName = context.getPackageName();
            console.log("[*] Auto-Detected Target Package: " + appPackageName);
        }
    } catch(e) { console.log("[!] Warning: Failed to get package name, using heuristic filter."); }
    console.log("[*] Monitoring active... (Filtering for anomalies only)");
    console.log("");

    const RequestBody = Java.use("okhttp3.RequestBody");
    const OkHttpClient = Java.use("okhttp3.OkHttpClient");
    const Log = Java.use("android.util.Log");
    const Exception = Java.use("java.lang.Exception");

    // --- HELPER FUNCTIONS ---

    function getTimestamp() { return new Date().toISOString(); }

    function getStackTrace() { return Log.getStackTraceString(Exception.$new()); }

    function analyzePayload(content) {
        const sensitiveKeywords = [
            "password", "passwd", "kennwort", "email", 
            "login", "token", "secret", "credential", "pin", "auth"
        ];
        let foundMatches = [];
        const lowerContent = content.toLowerCase();
        for (var i = 0; i < sensitiveKeywords.length; i++) {
            if (lowerContent.includes(sensitiveKeywords[i])) foundMatches.push(sensitiveKeywords[i]);
        }
        return foundMatches;
    }

    // --- HOOK A: Payload Inspection ---
    const hookBody = function(args, overloadType) {
        const content = (overloadType === 1) ? args[1] : args[0];
        const matches = analyzePayload(content);

        if (matches.length > 0) {
            console.log("\n[" + getTimestamp() + "] [+] SUSPICIOUS PAYLOAD CREATED");
            console.log("    ├── Trigger Keywords: " + JSON.stringify(matches));
            console.log("    ├── Content Preview:  " + content.substring(0, 150) + (content.length > 150 ? "..." : ""));
            
            // Stacktrace Filter
            let traceLines = getStackTrace().split("\n");
            let origin = "Unknown Source";
            for(let i=0; i<traceLines.length; i++) {
                if(appPackageName !== "Unknown" && traceLines[i].includes(appPackageName)) {
                    origin = traceLines[i].trim(); break;
                }
            }
            console.log("    └── Origin Trace:     " + origin); 
        }
    };

    try {
        RequestBody.create.overload('okhttp3.MediaType', 'java.lang.String').implementation = function (mt, c) {
            hookBody(arguments, 1); return this.create(mt, c);
        };
    } catch (e) {}
    try {
        RequestBody.create.overload('java.lang.String', 'okhttp3.MediaType').implementation = function (c, mt) {
            hookBody(arguments, 2); return this.create(c, mt);
        };
    } catch (e) {}


    // --- HOOK B: Traffic Anomaly Detection ---
    OkHttpClient.newCall.implementation = function (request) {
        const url = request.url().toString();
        
        const isCleartext = url.startsWith("http://");
        const ipv4Regex = /http:\/\/(?:[0-9]{1,3}\.){3}[0-9]{1,3}/;
        const ipv6Regex = /http:\/\/\[[a-fA-F0-9:]+\]/;
        
        if (isCleartext) {
            console.log("\n[" + getTimestamp() + "] [i] NETWORK ANOMALY: Outbound Request");
            console.log("    ├── Target URL: " + url);
            
            if (url.match(ipv4Regex) || url.match(ipv6Regex)) {
                console.log("    ├── Analysis:   RISK (Direct IP + Cleartext)");
            } else {
                console.log("    ├── Analysis:   RISK (Cleartext Traffic)");
            }
            
            let traceLines = getStackTrace().split("\n");
            let caller = "Unknown / External Lib";
            for(let i=0; i<traceLines.length; i++) {
                // Dynamischer Filter
                if(appPackageName !== "Unknown" && traceLines[i].includes(appPackageName)) {
                    caller = traceLines[i].trim(); break;
                }
                // Fallback
                else if (appPackageName === "Unknown" && !traceLines[i].includes("com.android") && !traceLines[i].includes("okhttp3") && !traceLines[i].includes("java.")) {
                    caller = traceLines[i].trim(); break;
                }
            }
            console.log("    └── Triggered by: " + caller);
        }

        return this.newCall(request);
    };
});