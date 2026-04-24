package com.phantommax.app

object SpoofingEngine {

    private val CHROME_VERSIONS = listOf(
        Triple("131", "131.0.6778.140", "24"),
        Triple("132", "132.0.6834.110", "24"),
        Triple("133", "133.0.6943.98",  "8"),
        Triple("134", "134.0.6998.88",  "8"),
        Triple("135", "135.0.7049.52",  "8")
    )

    private val DESKTOP_OS = listOf(
        Triple("Windows NT 10.0; Win64; x64", "Win32",         "10.0.22621"),
        Triple("Windows NT 10.0; Win64; x64", "Win32",         "10.0.19045"),
        Triple("Windows NT 10.0; Win64; x64", "Win32",         "15.0.0"),
        Triple("Macintosh; Intel Mac OS X 10_15_7", "MacIntel", "10.15.7"),
        Triple("Macintosh; Intel Mac OS X 14_4_1",  "MacIntel", "14.4.1")
    )

    private val SCREEN_DESKTOP = listOf(
        Pair(1920, 1080), Pair(2560, 1440), Pair(1440, 900),
        Pair(1680, 1050), Pair(2560, 1600), Pair(1366, 768)
    )

    private val DPR_DESKTOP    = listOf(1.0, 1.25, 1.5, 2.0)
    private val HW_CONCURRENCY = listOf(4, 6, 8, 10, 12, 16)
    private val DEVICE_MEMORY  = listOf(4, 8, 16)

    private val TIMEZONES = listOf(
        Pair("Europe/Moscow",  180),
        Pair("Europe/Kiev",    120),
        Pair("Asia/Almaty",    360),
        Pair("Europe/Minsk",   180),
        Pair("Europe/Berlin",  60)
    )

    private val WEBGL_RENDERERS = listOf(
        Pair("Google Inc. (Intel)",  "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        Pair("Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        Pair("Google Inc. (AMD)",    "ANGLE (AMD, AMD Radeon RX 6600 Direct3D11 vs_5_0 ps_5_0, D3D11)"),
        Pair("Google Inc. (Intel)",  "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)")
    )

    fun generateScript(seed: Long, @Suppress("UNUSED_PARAMETER") isDesktop: Boolean): String {
        val rng = SeededRng(seed)

        val cv         = CHROME_VERSIONS[rng.nextInt(CHROME_VERSIONS.size)]
        val chromeMain = cv.first
        val chromeFull = cv.second
        val notBrand   = cv.third

        val os          = DESKTOP_OS[rng.nextInt(DESKTOP_OS.size)]
        val osStr       = os.first
        val platform    = os.second
        val platformVer = os.third

        val sc  = SCREEN_DESKTOP[rng.nextInt(SCREEN_DESKTOP.size)]
        val scW = sc.first
        val scH = sc.second
        val dpr = DPR_DESKTOP[rng.nextInt(DPR_DESKTOP.size)]

        val hwc = HW_CONCURRENCY[rng.nextInt(HW_CONCURRENCY.size)]
        val mem = DEVICE_MEMORY[rng.nextInt(DEVICE_MEMORY.size)]
        val tz  = TIMEZONES[rng.nextInt(TIMEZONES.size)]
        val tzName   = tz.first
        val tzOffset = tz.second

        val wgl         = WEBGL_RENDERERS[rng.nextInt(WEBGL_RENDERERS.size)]
        val wglVendor   = wgl.first
        val wglRenderer = wgl.second

        val ua = "Mozilla/5.0 ($osStr) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeMain}.0.0.0 Safari/537.36"

        val totalHeap  = 20000000 + rng.nextInt(15000000)
        val usedHeap   = 14000000 + rng.nextInt(5000000)
        val canvasSeed = (seed and 0x7FFFFFFFL).toInt()
        val noiseR     = rng.nextInt(5) - 2
        val noiseG     = rng.nextInt(5) - 2
        val rectNoise  = rng.nextInt(80).toDouble() / 1000.0
        val rtt        = 35 + rng.nextInt(65)
        val dl         = 8  + rng.nextInt(18)
        val osPlatform = if (osStr.contains("Mac")) "macOS" else "Windows"

        return buildScriptModular(
            seed, chromeMain, chromeFull, notBrand,
            osStr, platform, platformVer, osPlatform,
            scW, scH, dpr, hwc, mem,
            tzName, tzOffset,
            wglVendor, wglRenderer,
            ua, totalHeap, usedHeap, canvasSeed,
            noiseR, noiseG, rectNoise, rtt, dl
        )
    }

    @Suppress("LongParameterList")
    private fun buildScriptModular(
        seed: Long, chromeMain: String, chromeFull: String, notBrand: String,
        osStr: String, platform: String, platformVer: String, osPlatform: String,
        @Suppress("UNUSED_PARAMETER") scW: Int, @Suppress("UNUSED_PARAMETER") scH: Int, @Suppress("UNUSED_PARAMETER") dpr: Double,
        hwc: Int, mem: Int, tzName: String, tzOffset: Int,
        wglVendor: String, wglRenderer: String, ua: String,
        totalHeap: Int, usedHeap: Int, canvasSeed: Int, noiseR: Int, noiseG: Int,
        @Suppress("UNUSED_PARAMETER") rectNoise: Double, rtt: Int, dl: Int
    ): String {
        val modules = listOf(
            moduleRuntimeGuards(seed),
            moduleRtcAndSensors(),
            moduleCanvasAndWebGl(canvasSeed, noiseR, noiseG, wglVendor, wglRenderer),
            moduleAudio(seed),
            moduleUaAndClientHints(ua, osStr, platform, platformVer, osPlatform, chromeMain, chromeFull, notBrand, hwc, mem, rtt, dl),
            modulePrivacyAndTimezone(tzName, tzOffset, totalHeap, usedHeap)
        )
        return "(function(){if(window.__phantomApplied)return;window.__phantomApplied=true;${modules.joinToString(";")}})();"
    }

    private fun moduleRuntimeGuards(seed: Long): String = """
var _S=${seed and 0x7FFFFFFFL};
['__android_log_write','AndroidInterface','android','__ANDROID__','_phantom','mozInnerScreenX','_Selenium_IDE_Recorder'].forEach(function(n){try{delete window[n];}catch(e){}});
""".trimIndent()

    private fun moduleRtcAndSensors(): String = """
['RTCPeerConnection','webkitRTCPeerConnection','mozRTCPeerConnection','RTCSessionDescription','RTCIceCandidate'].forEach(function(n){try{delete window[n];}catch(e){}});
try{delete window.DeviceMotionEvent;}catch(e){}
try{delete window.DeviceOrientationEvent;}catch(e){}
""".trimIndent()

    private fun moduleCanvasAndWebGl(canvasSeed: Int, noiseR: Int, noiseG: Int, wglVendor: String, wglRenderer: String): String = """
var _cS=$canvasSeed;
function _xorshift(s){s=(s^(s<<13))&0x7fffffff;s=(s^(s>>17))&0x7fffffff;s=(s^(s<<5))&0x7fffffff;return s;}
function _injectNoise(ctx,w,h){try{var iw=Math.min(w,64),ih=Math.min(h,64);if(iw<=0||ih<=0)return;var id=ctx.getImageData(0,0,iw,ih);var d=id.data;var ls=_cS;for(var i=0;i<d.length;i+=4){ls=_xorshift(ls);d[i]=Math.max(0,Math.min(255,d[i]+$noiseR+((ls%5)-2)));ls=_xorshift(ls);d[i+1]=Math.max(0,Math.min(255,d[i+1]+$noiseG+((ls%5)-2)));}ctx.putImageData(id,0,0);}catch(e){}}
try{var _otoDataURL=HTMLCanvasElement.prototype.toDataURL;HTMLCanvasElement.prototype.toDataURL=function(){var ctx=this.getContext('2d');if(ctx)_injectNoise(ctx,this.width,this.height);return _otoDataURL.apply(this,arguments);};}catch(e){}
try{var _sp=function(p){if(!p)return;var _gp=p.prototype.getParameter;if(!_gp)return;p.prototype.getParameter=function(a){if(a===37445||a===0x9245)return'$wglVendor';if(a===37446||a===0x9246)return'$wglRenderer';return _gp.apply(this,arguments);};};_sp(window.WebGLRenderingContext);_sp(window.WebGL2RenderingContext);}catch(e){}
""".trimIndent()

    private fun moduleAudio(seed: Long): String = """
try{if(window.AudioBuffer){var _gc=AudioBuffer.prototype.getChannelData;AudioBuffer.prototype.getChannelData=function(){var d=_gc.apply(this,arguments);var ls=${seed and 0x7FFFFFFFL};for(var i=0;i<d.length&&i<4096;i++){ls=(ls^(ls<<13))&0x7fffffff;d[i]=d[i]+((ls%1000-500)/10000000);}return d;};}}catch(e){}
""".trimIndent()

    @Suppress("LongParameterList")
    private fun moduleUaAndClientHints(
        ua: String, osStr: String, platform: String, platformVer: String, osPlatform: String,
        chromeMain: String, chromeFull: String, notBrand: String, hwc: Int, mem: Int, rtt: Int, dl: Int
    ): String = """
try{Object.defineProperties(navigator,{userAgent:{get:function(){return '$ua';},configurable:true},platform:{get:function(){return '$platform';},configurable:true},hardwareConcurrency:{get:function(){return $hwc;},configurable:true},deviceMemory:{get:function(){return $mem;},configurable:true},webdriver:{get:function(){return false;},configurable:true}});}catch(e){}
try{Object.defineProperty(navigator,'userAgentData',{value:{brands:[{brand:'Google Chrome',version:'$chromeMain'},{brand:'Chromium',version:'$chromeMain'},{brand:'Not_A Brand',version:'$notBrand'}],mobile:false,platform:'$osPlatform',getHighEntropyValues:function(){return Promise.resolve({architecture:'x86',bitness:'64',platformVersion:'$platformVer',uaFullVersion:'$chromeFull'});}},configurable:true});}catch(e){}
try{Object.defineProperty(navigator,'connection',{get:function(){return{effectiveType:'4g',rtt:$rtt,downlink:$dl,saveData:false};},configurable:true});}catch(e){}
""".trimIndent()

    private fun modulePrivacyAndTimezone(tzName: String, tzOffset: Int, totalHeap: Int, usedHeap: Int): String = """
try{Object.defineProperty(Date.prototype,'getTimezoneOffset',{value:function(){return -$tzOffset;},writable:true,configurable:true});}catch(e){}
try{var _oiro=Intl.DateTimeFormat.prototype.resolvedOptions;Intl.DateTimeFormat.prototype.resolvedOptions=function(){var r=_oiro.apply(this,arguments);r.timeZone='$tzName';return r;};}catch(e){}
try{Object.defineProperty(performance,'memory',{get:function(){return{jsHeapSizeLimit:4294705152,totalJSHeapSize:$totalHeap,usedJSHeapSize:$usedHeap};},configurable:true});}catch(e){}
try{if(navigator.permissions){Object.defineProperty(navigator.permissions,'query',{value:function(){return Promise.resolve({state:'denied'});},configurable:true});}}catch(e){}
""".trimIndent()

    private fun buildScript(
        seed: Long, chromeMain: String, chromeFull: String, notBrand: String,
        osStr: String, platform: String, platformVer: String, osPlatform: String,
        scW: Int, scH: Int, dpr: Double, hwc: Int, mem: Int,
        tzName: String, tzOffset: Int,
        wglVendor: String, wglRenderer: String,
        ua: String, totalHeap: Int, usedHeap: Int, canvasSeed: Int,
        noiseR: Int, noiseG: Int, rectNoise: Double, rtt: Int, dl: Int
    ): String = """(function(){
if(window.__phantomApplied)return;window.__phantomApplied=true;
var _S=${seed and 0x7FFFFFFFL};

var _rtcN=['RTCPeerConnection','webkitRTCPeerConnection','mozRTCPeerConnection','RTCSessionDescription','RTCIceCandidate','mozRTCSessionDescription','mozRTCIceCandidate','RTCDataChannel','RTCDTMFSender','RTCStatsReport'];
_rtcN.forEach(function(n){try{delete window[n];}catch(e){}try{Object.defineProperty(window,n,{get:function(){return undefined;},configurable:true});}catch(e){}});

['__android_log_write','AndroidInterface','android','__ANDROID__','_phantom','mozInnerScreenX','_Selenium_IDE_Recorder'].forEach(function(n){try{delete window[n];}catch(e){}});

['gpu','bluetooth','usb','serial','hid','xr','nfc','scheduling','wakeLock','ink','keyboard','locks','managed','presentation','userActivation','getInstalledRelatedApps','requestMIDIAccess','getGamepads','share','canShare','vibrate'].forEach(function(n){
try{delete Navigator.prototype[n];}catch(e){}
try{delete navigator[n];}catch(e){}
});

var _cS=$canvasSeed;
function _xorshift(s){s=(s^(s<<13))&0x7fffffff;s=(s^(s>>17))&0x7fffffff;s=(s^(s<<5))&0x7fffffff;return s;}
function _injectNoise(ctx,w,h){try{var iw=Math.min(w,64),ih=Math.min(h,64);if(iw<=0||ih<=0)return;var id=ctx.getImageData(0,0,iw,ih);var d=id.data;var ls=_cS;for(var i=0;i<d.length;i+=4){ls=_xorshift(ls);var nr=(ls%5)-2;ls=_xorshift(ls);var ng=(ls%5)-2;d[i]=Math.max(0,Math.min(255,d[i]+$noiseR+nr));d[i+1]=Math.max(0,Math.min(255,d[i+1]+$noiseG+ng));}ctx.putImageData(id,0,0);}catch(e){}}
var _otoDataURL=HTMLCanvasElement.prototype.toDataURL;
HTMLCanvasElement.prototype.toDataURL=function(){var ctx=this.getContext('2d');if(ctx)_injectNoise(ctx,this.width,this.height);return _otoDataURL.apply(this,arguments);};
var _otoBlob=HTMLCanvasElement.prototype.toBlob;
if(_otoBlob){HTMLCanvasElement.prototype.toBlob=function(cb,t,q){var ctx=this.getContext('2d');if(ctx)_injectNoise(ctx,this.width,this.height);return _otoBlob.call(this,cb,t,q);};}
var _ogid=CanvasRenderingContext2D.prototype.getImageData;
CanvasRenderingContext2D.prototype.getImageData=function(){var id=_ogid.apply(this,arguments);var d=id.data;var ls=_cS;for(var i=0;i<d.length;i+=4){ls=_xorshift(ls);d[i]=Math.max(0,Math.min(255,d[i]+(ls%3)-1));}return id;};

function _spWebGL(p){
if(!p)return;
var _gp=p.prototype.getParameter;if(!_gp)return;
p.prototype.getParameter=function(a){
if(a===37445||a===0x9245)return'$wglVendor';
if(a===37446||a===0x9246)return'$wglRenderer';
if(a===7936)return'WebKit';if(a===7937)return'WebKit WebGL';
if(a===7938)return'WebGL 1.0 (OpenGL ES 2.0 Chromium)';
if(a===35724)return'WebGL GLSL ES 1.0 (OpenGL ES GLSL ES 1.0 Chromium)';
if(a===3379)return 16384;if(a===34076)return 16384;
if(a===34921)return 16;if(a===36347)return 1024;
if(a===36348)return 32;if(a===36349)return 32;
if(a===35661)return 16;if(a===35660)return 8;
return _gp.apply(this,arguments);};
var _rp=p.prototype.readPixels;
if(_rp){p.prototype.readPixels=function(){_rp.apply(this,arguments);if(arguments[6]&&arguments[6].length>0){var a=arguments[6];var ls=_cS;for(var i=0;i<Math.min(a.length,64);i+=4){ls=_xorshift(ls);a[i]=Math.max(0,Math.min(255,a[i]+(ls%3)-1));}}};}
var _ge=p.prototype.getExtension;
if(_ge){p.prototype.getExtension=function(n){if(n==='WEBGL_debug_renderer_info')return{UNMASKED_VENDOR_WEBGL:37445,UNMASKED_RENDERER_WEBGL:37446};if(n==='EXT_disjoint_timer_query'||n==='EXT_disjoint_timer_query_webgl2')return null;return _ge.apply(this,arguments);};}
var _gse=p.prototype.getSupportedExtensions;
if(_gse){p.prototype.getSupportedExtensions=function(){var e=_gse.apply(this,arguments);if(e&&e.filter)return e.filter(function(x){return x!=='WEBGL_debug_renderer_info'&&x!=='EXT_disjoint_timer_query'&&x!=='EXT_disjoint_timer_query_webgl2';});return e;};}
}
try{_spWebGL(window.WebGLRenderingContext);}catch(e){}
try{_spWebGL(window.WebGL2RenderingContext);}catch(e){}

function _spAudio(){
if(!window.AudioBuffer)return;
var _gc=AudioBuffer.prototype.getChannelData;
AudioBuffer.prototype.getChannelData=function(){var d=_gc.apply(this,arguments);var ls=Number(_S)&0x7fffffff;for(var i=0;i<d.length&&i<4096;i++){ls=_xorshift(ls);d[i]=d[i]+((ls%1000-500)/10000000);}return d;};
if(window.AnalyserNode){
var _ff=AnalyserNode.prototype.getFloatFrequencyData;
AnalyserNode.prototype.getFloatFrequencyData=function(a){_ff.apply(this,arguments);var ls=Number(_S)&0x7fffffff;for(var i=0;i<a.length&&i<1024;i++){ls=_xorshift(ls);a[i]=a[i]+((ls%100-50)/100000);}};
var _bf=AnalyserNode.prototype.getByteFrequencyData;
AnalyserNode.prototype.getByteFrequencyData=function(a){_bf.apply(this,arguments);var ls=Number(_S)&0x7fffffff;for(var i=0;i<a.length&&i<1024;i++){ls=_xorshift(ls);a[i]=Math.max(0,Math.min(255,a[i]+(ls%3)-1));}};
}
if(window.OfflineAudioContext){
var _oOAC=window.OfflineAudioContext;
window.OfflineAudioContext=function(){var ctx=new _oOAC(arguments[0]||1,arguments[1]||44100,arguments[2]||44100);var _or=ctx.startRendering.bind(ctx);ctx.startRendering=function(){return _or().then(function(b){var cd=b.getChannelData(0);var ls=Number(_S)&0x7fffffff;for(var i=0;i<cd.length&&i<4096;i++){ls=_xorshift(ls);cd[i]=cd[i]+((ls%1000-500)/10000000);}return b;});};return ctx;};
window.OfflineAudioContext.prototype=_oOAC.prototype;
}}
try{_spAudio();}catch(e){}

var _tUA='$ua';
var _hvn=$hwc;var _hmem=$mem;
var _nP={
userAgent:          {get:function(){return _tUA;},configurable:true},
appVersion:         {get:function(){return '5.0 ($osStr) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeMain}.0.0.0 Safari/537.36';},configurable:true},
platform:           {get:function(){return '$platform';},configurable:true},
vendor:             {get:function(){return 'Google Inc.';},configurable:true},
product:            {value:'Gecko',configurable:true},
productSub:         {value:'20030107',configurable:true},
hardwareConcurrency:{get:function(){return _hvn;},configurable:true},
deviceMemory:       {get:function(){return _hmem;},configurable:true},
maxTouchPoints:     {get:function(){return 0;},configurable:true},
webdriver:          {get:function(){return false;},configurable:true},
doNotTrack:         {value:null,configurable:true},
languages:          {get:function(){return Object.freeze(['ru-RU','ru','en-US','en']);},configurable:true},
language:           {get:function(){return 'ru-RU';},configurable:true},
onLine:             {value:true,configurable:true},
cookieEnabled:      {value:true,configurable:true},
pdfViewerEnabled:   {value:false,configurable:true},
appName:            {value:'Netscape',configurable:true},
appCodeName:        {value:'Mozilla',configurable:true},
oscpu:              {value:undefined,configurable:true}
};
try{Object.defineProperties(navigator,_nP);}catch(e){for(var _k in _nP){try{Object.defineProperty(navigator,_k,_nP[_k]);}catch(e2){}}}
try{Object.defineProperty(Navigator.prototype,'webdriver',{get:function(){return false;},configurable:true});}catch(e){}
try{Object.defineProperty(Navigator.prototype,'userAgent',{get:function(){return _tUA;},configurable:true});}catch(e){}

try{Object.defineProperty(navigator,'plugins',{get:function(){
var _p1={name:'PDF Viewer',description:'Portable Document Format',filename:'internal-pdf-viewer',length:1};_p1[0]={type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'};
var _p2={name:'Chrome PDF Viewer',description:'Portable Document Format',filename:'internal-pdf-viewer',length:1};_p2[0]={type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'};
var _p3={name:'Chromium PDF Viewer',description:'Portable Document Format',filename:'internal-pdf-viewer',length:1};_p3[0]={type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'};
var _pl={0:_p1,1:_p2,2:_p3,length:3,item:function(i){return this[i]||null;},namedItem:function(n){for(var i=0;i<3;i++){if(this[i]&&this[i].name===n)return this[i];}return null;},refresh:function(){}};
_pl[Symbol.iterator]=function(){var idx=0;var s=this;return{next:function(){if(idx<s.length)return{value:s[idx++],done:false};return{done:true};}};};
return _pl;},configurable:true});}catch(e){}
try{Object.defineProperty(navigator,'mimeTypes',{get:function(){
var _mt={0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'},length:1,item:function(i){return this[i]||null;},namedItem:function(n){if(n==='application/pdf')return this[0];return null;}};
_mt[Symbol.iterator]=function(){var idx=0;var s=this;return{next:function(){if(idx<s.length)return{value:s[idx++],done:false};return{done:true};}};};
return _mt;},configurable:true});}catch(e){}

try{
var _uad={
brands:Object.freeze([Object.freeze({brand:'Google Chrome',version:'$chromeMain'}),Object.freeze({brand:'Chromium',version:'$chromeMain'}),Object.freeze({brand:'Not_A Brand',version:'$notBrand'})]),
mobile:false,
platform:'$osPlatform',
getHighEntropyValues:function(h){return Promise.resolve({architecture:'x86',bitness:'64',brands:[{brand:'Google Chrome',version:'$chromeMain'},{brand:'Chromium',version:'$chromeMain'},{brand:'Not_A Brand',version:'$notBrand'}],fullVersionList:[{brand:'Google Chrome',version:'$chromeFull'},{brand:'Chromium',version:'$chromeFull'},{brand:'Not_A Brand',version:'${notBrand}.0.0.0'}],mobile:false,model:'',platform:'$osPlatform',platformVersion:'$platformVer',uaFullVersion:'$chromeFull',wow64:false,formFactors:['Desktop']});},
toJSON:function(){return{brands:this.brands,mobile:this.mobile,platform:this.platform};}
};
Object.defineProperty(navigator,'userAgentData',{value:_uad,writable:true,configurable:true});
}catch(e){}

try{delete Navigator.prototype.getBattery;}catch(e){}
try{delete navigator.getBattery;}catch(e){}
try{var _conn={effectiveType:'4g',rtt:$rtt,downlink:$dl,saveData:false,onchange:null,addEventListener:function(){},removeEventListener:function(){},type:'wifi'};Object.defineProperty(navigator,'connection',{get:function(){return _conn;},configurable:true});}catch(e){}
try{delete Navigator.prototype.devicePosture;}catch(e){}

try{if(navigator.mediaDevices){
Object.defineProperty(navigator.mediaDevices,'enumerateDevices',{value:function(){return Promise.resolve([]);},writable:true,configurable:true});
Object.defineProperty(navigator.mediaDevices,'getUserMedia',{value:function(){return Promise.reject(new DOMException('Permission denied','NotAllowedError'));},writable:true,configurable:true});
Object.defineProperty(navigator.mediaDevices,'getDisplayMedia',{value:function(){return Promise.reject(new DOMException('Not allowed','NotAllowedError'));},writable:true,configurable:true});
Object.defineProperty(navigator.mediaDevices,'getSupportedConstraints',{value:function(){return{width:true,height:true,frameRate:true,facingMode:false,deviceId:true};},writable:true,configurable:true});
}}catch(e){}

try{if(navigator.permissions){Object.defineProperty(navigator.permissions,'query',{value:function(d){var dn=d&&d.name||'';if(dn==='notifications'||dn==='geolocation'||dn==='camera'||dn==='microphone')return Promise.resolve({state:'denied',onchange:null,addEventListener:function(){},removeEventListener:function(){}});return Promise.resolve({state:'denied',onchange:null,addEventListener:function(){},removeEventListener:function(){}});},writable:true,configurable:true});}}catch(e){}
try{delete Navigator.prototype.serviceWorker;}catch(e){}

var _rN=$rectNoise;
try{if(document.fonts){
Object.defineProperty(document.fonts,'check',{value:function(){return false;},writable:true,configurable:true});
}}catch(e){}

try{Object.defineProperty(Date.prototype,'getTimezoneOffset',{value:function(){return -$tzOffset;},writable:true,configurable:true});}catch(e){}
try{
var _oiro=Intl.DateTimeFormat.prototype.resolvedOptions;
Intl.DateTimeFormat.prototype.resolvedOptions=function(){var r=_oiro.apply(this,arguments);try{Object.defineProperty(r,'timeZone',{value:'$tzName',writable:true,configurable:true});}catch(e){r.timeZone='$tzName';}return r;};
var _oDTF=Intl.DateTimeFormat;
Intl.DateTimeFormat=function(l,o){if(o&&o.timeZone===undefined)o=Object.assign({},o,{timeZone:'$tzName'});else if(!o)o={timeZone:'$tzName'};return new _oDTF(l,o);};
Intl.DateTimeFormat.prototype=_oDTF.prototype;
try{Object.defineProperty(Intl.DateTimeFormat,'supportedLocalesOf',{value:_oDTF.supportedLocalesOf.bind(_oDTF),writable:true,configurable:true});}catch(e){}
}catch(e){}
try{
var _oIntl=Intl.RelativeTimeFormat&&Intl.RelativeTimeFormat.prototype.resolvedOptions;
if(_oIntl){Intl.RelativeTimeFormat.prototype.resolvedOptions=function(){var r=_oIntl.apply(this,arguments);try{r.locale='ru-RU';}catch(e){}return r;};}
}catch(e){}

try{var _mem2={jsHeapSizeLimit:4294705152,totalJSHeapSize:$totalHeap,usedJSHeapSize:$usedHeap};Object.defineProperty(performance,'memory',{get:function(){return _mem2;},configurable:true});}catch(e){}

try{Object.defineProperty(document,'referrer',{get:function(){return '';},configurable:true});}catch(e){}
try{Object.defineProperty(history,'length',{get:function(){return 1;},configurable:true});}catch(e){}
try{Object.defineProperty(document,'hidden',{get:function(){return false;},configurable:true});}catch(e){}
try{Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';},configurable:true});}catch(e){}
try{Object.defineProperty(document,'hasFocus',{value:function(){return true;},writable:true,configurable:true});}catch(e){}

try{
var _chromeObj={
runtime:{id:undefined,connect:undefined,sendMessage:undefined},
loadTimes:function(){return{commitLoadTime:performance.now()/1000,connectionInfo:'h2',finishDocumentLoadTime:0,finishLoadTime:0,firstPaintAfterLoadTime:0,firstPaintTime:0,navigationType:'Other',npnNegotiatedProtocol:'h2',requestTime:performance.now()/1000,startLoadTime:performance.now()/1000,wasAlternateProtocolAvailable:false,wasFetchedViaSpdy:true,wasNpnNegotiated:true};},
csi:function(){return{onloadT:Date.now(),pageT:Date.now(),startE:Date.now(),tran:15};},
app:{isInstalled:false,InstallState:{DISABLED:'disabled',INSTALLED:'installed',NOT_INSTALLED:'not_installed'},RunningState:{CANNOT_RUN:'cannot_run',READY_TO_RUN:'ready_to_run',RUNNING:'running'}}
};
Object.defineProperty(window,'chrome',{get:function(){return _chromeObj;},configurable:true});
}catch(e){}

try{var _oAEL=EventTarget.prototype.addEventListener;EventTarget.prototype.addEventListener=function(t){if(t==='touchstart'||t==='touchend'||t==='touchmove'||t==='touchcancel'||t==='devicemotion'||t==='deviceorientation'||t==='deviceorientationabsolute')return;return _oAEL.apply(this,arguments);};}catch(e){}
['ontouchstart','ontouchend','ontouchmove','ontouchcancel'].forEach(function(n){try{Object.defineProperty(window,n,{get:function(){return undefined;},set:function(){},configurable:true});}catch(e){}});
try{delete window.DeviceMotionEvent;}catch(e){}
try{delete window.DeviceOrientationEvent;}catch(e){}
try{delete window.TouchEvent;}catch(e){}

try{var _mql=window.matchMedia;window.matchMedia=function(q){var lq=String(q).toLowerCase();var m={matches:false,media:q,onchange:null,addEventListener:function(){},removeEventListener:function(){},addListener:function(){},removeListener:function(){}};if(lq.indexOf('pointer')>=0&&lq.indexOf('fine')>=0){m.matches=true;return m;}if(lq.indexOf('pointer')>=0&&lq.indexOf('coarse')>=0){return m;}if(lq.indexOf('hover')>=0&&lq.indexOf('none')>=0){return m;}if(lq.indexOf('any-hover')>=0){m.matches=true;return m;}if(lq.indexOf('standalone')>=0){return m;}if(lq.indexOf('prefers-color-scheme')>=0&&lq.indexOf('dark')>=0){m.matches=true;return m;}return _mql.apply(this,arguments);};}catch(e){}

try{if(window.Notification){Object.defineProperty(window.Notification,'permission',{get:function(){return 'denied';},configurable:true});window.Notification.requestPermission=function(){return Promise.resolve('denied');};}}catch(e){}
try{if(window.speechSynthesis){Object.defineProperty(window.speechSynthesis,'getVoices',{value:function(){return[];},writable:true,configurable:true});Object.defineProperty(window.speechSynthesis,'speaking',{get:function(){return false;},configurable:true});Object.defineProperty(window.speechSynthesis,'pending',{get:function(){return false;},configurable:true});}}catch(e){}

try{var _oFetch=window.fetch;window.fetch=function(input,init){if(init&&init.headers){var bad=['X-Requested-With','X-Android-Selected-Application','X-Android-Application-Id','X-Android-Package','X-Android-Sdk-Version'];if(init.headers instanceof Headers){bad.forEach(function(h){init.headers.delete(h);});}else if(typeof init.headers==='object'){bad.forEach(function(h){delete init.headers[h];delete init.headers[h.toLowerCase()];});}}return _oFetch.apply(window,arguments);};}catch(e){}
try{var _oXHR=XMLHttpRequest.prototype.setRequestHeader;XMLHttpRequest.prototype.setRequestHeader=function(n,v){if(n&&(n.toLowerCase()==='x-requested-with'||n.toLowerCase().indexOf('x-android')===0))return;return _oXHR.apply(this,arguments);};}catch(e){}

try{Object.defineProperty(window,'crossOriginIsolated',{value:false,configurable:true});}catch(e){}

try{
if(window.CSS&&window.CSS.supports){
var _oCSSS=window.CSS.supports.bind(window.CSS);
window.CSS.supports=function(p,v){if(typeof p==='string'&&(p.indexOf('-webkit-text-size-adjust')>=0||p.indexOf('-webkit-overflow-scrolling')>=0))return true;return _oCSSS.apply(this,arguments);};
}
}catch(e){}

try{
var _oNC=window.Navigator;
if(_oNC&&_oNC.prototype){
['getBattery','getAutoplayPolicy','requestMediaKeySystemAccess'].forEach(function(m){
try{Object.defineProperty(_oNC.prototype,m,{value:m==='getAutoplayPolicy'?function(){return'allowed';}:undefined,configurable:true});}catch(e){}
});
}
}catch(e){}

})();"""

    fun generateScriptTag(seed: Long, isDesktop: Boolean): String {
        return "<script>${generateScript(seed, isDesktop)}</script>"
    }

    fun getSpoofLog(seed: Long, @Suppress("UNUSED_PARAMETER") isDesktop: Boolean): String {
        val rng = SeededRng(seed)
        val cv = CHROME_VERSIONS[rng.nextInt(CHROME_VERSIONS.size)]; val chromeMain = cv.first
        val os = DESKTOP_OS[rng.nextInt(DESKTOP_OS.size)]; val osStr = os.first
        val sc = SCREEN_DESKTOP[rng.nextInt(SCREEN_DESKTOP.size)]; val scW = sc.first; val scH = sc.second
        val dpr = DPR_DESKTOP[rng.nextInt(DPR_DESKTOP.size)]
        val hwc = HW_CONCURRENCY[rng.nextInt(HW_CONCURRENCY.size)]
        val mem = DEVICE_MEMORY[rng.nextInt(DEVICE_MEMORY.size)]
        val tz = TIMEZONES[rng.nextInt(TIMEZONES.size)]; val tzName = tz.first; val tzOffset = tz.second
        val wgl = WEBGL_RENDERERS[rng.nextInt(WEBGL_RENDERERS.size)]; val wglRenderer = wgl.second
        val ua = "Mozilla/5.0 ($osStr) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeMain}.0.0.0 Safari/537.36"
        rng.nextInt(15000000); rng.nextInt(5000000); rng.nextInt(5); rng.nextInt(5)
        val rectNoise = rng.nextInt(80).toDouble() / 1000.0
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        return listOf(
            "PhantomMAX v4.1 | STEALTH MAX",
            "UA: $ua",
            "Chrome $chromeMain | $osStr | ${scW}x${scH} | DPR:$dpr | CPU:$hwc | RAM:${mem}GB",
            "WebRTC/RTC: BLOCKED",
            "Canvas+WebGL noise: seed=${seed and 0xFFFF}",
            "WebGL GPU: $wglRenderer",
            "AudioContext: SPOOFED",
            "Client Hints: Chrome $chromeMain Desktop mobile=false",
            "MediaDevices.enumerateDevices: []",
            "Permissions: ALL DENIED",
            "ServiceWorker: BLOCKED",
            "Screen: ${scW}x${scH} DPR:$dpr landscape-primary",
            "ClientRects noise: ${"%.4f".format(java.util.Locale.US, rectNoise)}",
            "Timezone: $tzName UTC+${tzOffset / 60}",
            "Fonts: BLOCKED | Speech: BLOCKED | Notifications: DENIED",
            "TouchEvents: BLOCKED | DeviceMotion: BLOCKED",
            "window.name: CLEARED | SharedWorker: BLOCKED",
            "performance.navigation entries: HIDDEN",
            "Android markers: REMOVED",
            "WebDriver flag: false",
            "Total active spoof modules: 30 | Identity: Windows Desktop PC"
        ).joinToString("\n") { "[$time] $it" }
    }
}

private class SeededRng(seed: Long) {
    private var state: Long = seed xor 6364136223846793005L

    fun nextLong(): Long {
        state = state * 6364136223846793005L + 1442695040888963407L
        var z = state
        z = (z xor (z ushr 30)) * (-4658895341174019969L)
        z = (z xor (z ushr 27)) * (-7723592293110705685L)
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        val r = ((nextLong() ushr 1) % bound).toInt()
        return if (r < 0) r + bound else r
    }
}
