import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:nuova_tebe_installatore/deviceBE.dart';

class ScanPage extends StatefulWidget
{
  @override
  State<ScanPage> createState() => _ScanPageState();
}

class _ScanPageState extends State<ScanPage> {
FlutterBluePlus flutterBlue = FlutterBluePlus.instance;
List<ScanResult> scannedDevices=[];
bool isScanning=true;
@override
  void initState() {
    super.initState();
    // Start scanning
flutterBlue.startScan(timeout: Duration(seconds: 10)).then((value) =>   setState(() {
            isScanning=false;
          }));

// Listen to scan results
var subscription = flutterBlue.scanResults.listen((results) {
    // do something with scan results
    for (ScanResult r in results) {
        print('${r.device.name} found! rssi: ${r.rssi}');
        if(!scannedDevices.contains(r))
        {
          setState(() {
            scannedDevices.add(r);
          });
          }

    }
});

/*Future.delayed(Duration(seconds: 15)).then((value) {
  

  });
*/




  }
      static const platform = MethodChannel('com.example.nuova_tebe_installatore/GATTserver');


  @override
  Widget build(BuildContext context) {
    return Scaffold(
appBar: AppBar(title: AutoSizeText("Ricerca dispositivi Saet"),
backgroundColor: Colors.grey.shade400,
actions: [
  InkWell(
    child: Icon(Icons.disabled_by_default),
    onTap: (){

    }),
  InkWell(
    onTap: (){
      if(isScanning){
        flutterBlue.stopScan();
  setState(() {
            isScanning=false;
          });
      }
      else
      {
flutterBlue.startScan(timeout: Duration(seconds: 10)).then((value) =>   setState(() {
            isScanning=false;
          }));
  setState(() {
            isScanning=true;
          });
// Listen to scan results
var subscription = flutterBlue.scanResults.listen((results) {
    // do something with scan results
    for (ScanResult r in results) {
        print('${r.device.name} found! rssi: ${r.rssi}');
        if(!scannedDevices.contains(r))
        {
          setState(() {
            scannedDevices.add(r);
          });
          }

    }

});
      }
    },
    child: Icon(isScanning? Icons.bluetooth_searching_rounded : Icons.bluetooth_disabled_rounded),)
],
),
body: ListView(children: [
  ...scannedDevices.map(((e) =>   InkWell(
    onTap: () async {
        flutterBlue.stopScan();
             String bll= await platform.invokeMethod('startGattServer');
             print("BLLLLLLL BELLA BLLLLLL");

                                await e.device.connect();
                                await e.device.requestMtu(240);

 e.device.discoverServices().then((value) =>  Navigator.of(context)
                              .push(MaterialPageRoute(builder: (context) {
                            return DeviceBE(deviceServices: value, device: e.device);
                          })));

     // await e.device.connect();
      print("CONNESSO AL DEVICE");

    },
    child: 
           !e.device.name.isEmpty?

    Container(
      height:   e.device.name.toString().toUpperCase().contains("SAET")? 100 : 30,
      margin: EdgeInsets.all(e.device.name.toString().toUpperCase().contains("SAET")? 20 :0),
      decoration: BoxDecoration(
              color: Colors.grey.shade200,

        borderRadius:  e.device.name.toString().toUpperCase().contains("SAET")?  BorderRadius.circular(40) : BorderRadius.circular(0) ),
      child: 
      Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        Text(e.device.name, style: TextStyle(fontWeight : e.device.name.toString().toUpperCase().contains("SAET")? FontWeight.bold : FontWeight.w300),),
        Text(e.rssi.toString())
      ],
    )
  
    )
      :
    SizedBox()
    ,
  )))


],),

    );
  }
}