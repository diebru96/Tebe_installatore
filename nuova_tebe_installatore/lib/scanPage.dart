import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:nuova_tebe_installatore/deviceBE.dart';
import 'package:nuova_tebe_installatore/main.dart';
import 'package:nuova_tebe_installatore/neumorphic_button.dart';

class ScanPage extends StatefulWidget {
  @override
  State<ScanPage> createState() => _ScanPageState();
}

class _ScanPageState extends State<ScanPage> {
  FlutterBluePlus flutterBlue = FlutterBluePlus.instance;
  List<ScanResult> scannedDevices = [];
  bool isScanning = true;
  bool loading = false;
  @override
  void initState() {
    super.initState();
    // Start scanning
    flutterBlue
        .startScan(timeout: Duration(seconds: 10))
        .then((value) => setState(() {
              isScanning = false;
            }));

// Listen to scan results
    var subscription = flutterBlue.scanResults.listen((results) {
      // do something with scan results
      for (ScanResult r in results) {
        print('${r.device.name} found! rssi: ${r.rssi}');
        if (!scannedDevices.contains(r)) {
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

  static const platform =
      MethodChannel('com.example.nuova_tebe_installatore/GATTserver');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        foregroundColor: singleton.primaryColor,
        title: AutoSizeText("Ricerca dispositivi Saet"),
        backgroundColor: singleton.backgroundColor,
        elevation: 0.5,
        actions: [
          InkWell(
            onTap: () {
              if (isScanning) {
                flutterBlue.stopScan();
                setState(() {
                  isScanning = false;
                });
              } else {
                flutterBlue
                    .startScan(timeout: Duration(seconds: 10))
                    .then((value) => setState(() {
                          isScanning = false;
                        }));
                setState(() {
                  isScanning = true;
                });
// Listen to scan results
                var subscription = flutterBlue.scanResults.listen((results) {
                  // do something with scan results
                  for (ScanResult r in results) {
                    print('${r.device.name} found! rssi: ${r.rssi}');
                    if (!scannedDevices.contains(r)) {
                      setState(() {
                        scannedDevices.add(r);
                      });
                    }
                  }
                });
              }
            },
            child: Icon(isScanning
                ? Icons.bluetooth_searching_rounded
                : Icons.bluetooth_disabled_rounded),
          )
        ],
      ),
      backgroundColor: singleton.backgroundColor,
      body: Stack(
        children: [
          Column(
            children: [
              Container(
                decoration: BoxDecoration(
                    border: Border(
                        bottom:
                            BorderSide(color: singleton.greyText, width: 0.5))),
                height: 35,
                width: MediaQuery.of(context).size.width * 0.75,
                margin: EdgeInsets.fromLTRB(20, 10, 20, 0),
                padding: EdgeInsets.fromLTRB(10, 2, 10, 8),
                child: Center(
                  child: AutoSizeText(
                    "Dispositivi trovati: ${scannedDevices.length}",
                    style: TextStyle(color: singleton.greyText),
                  ),
                ),
              ),
              Expanded(
                child: ListView(
                  children: [
                    Container(
                      margin: EdgeInsets.fromLTRB(10, 15, 10, 15),
                      width: MediaQuery.of(context).size.width,
                      child: Row(
                        children: [
                          Expanded(
                            flex: 5,
                            child: SizedBox(),
                          ),
                          Expanded(
                            flex: 2,
                            child: Text(
                              "RSSI",
                              style: TextStyle(color: singleton.greyText),
                            ),
                          )
                        ],
                      ),
                    ),
                    ...scannedDevices.map(((e) => e.device.name.isNotEmpty
                        ? NeumorphicButton(
                            notTappable: !e.device.name
                                    .toString()
                                    .toUpperCase()
                                    .contains("SAET") &&
                                !loading,
                            internal: !e.device.name
                                .toString()
                                .toUpperCase()
                                .contains("SAET"),
                            action: () async {
                              flutterBlue.stopScan();
                              setState(() {
                                loading = true;
                              });
                              String bll = await platform
                                  .invokeMethod('startGattServer');

                              await e.device.connect();
                              await e.device.requestMtu(240);

                              e.device.discoverServices().then((value) {
                                setState(() {
                                  loading = false;
                                });
                                Navigator.of(context)
                                    .push(MaterialPageRoute(builder: (context) {
                                  return DeviceBE(
                                      deviceServices: value, device: e.device);
                                }));
                              });

                              // await e.device.connect();

                              print("CONNESSO AL DEVICE");
                            },
                            child: Container(
                                height: e.device.name
                                        .toString()
                                        .toUpperCase()
                                        .contains("SAET")
                                    ? 50
                                    : 45,
                                //    margin: EdgeInsets.all(e.device.name.toString().toUpperCase().contains("SAET")? 20 :0),
                                /*    decoration: BoxDecoration(
                        color: Colors.grey.shade200,
                
                  borderRadius:  e.device.name.toString().toUpperCase().contains("SAET")?  BorderRadius.circular(40) : BorderRadius.circular(0) ),
               */

                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceEvenly,
                                  children: [
                                    Expanded(
                                        flex: 3,
                                        child: Padding(
                                          padding:
                                              const EdgeInsets.only(left: 30.0),
                                          child: AutoSizeText(
                                            e.device.name,
                                            style: TextStyle(
                                                fontWeight: e.device.name
                                                        .toString()
                                                        .toUpperCase()
                                                        .contains("SAET")
                                                    ? FontWeight.bold
                                                    : FontWeight.w300,
                                                color: e.device.name
                                                        .toString()
                                                        .toUpperCase()
                                                        .contains("SAET")
                                                    ? singleton.primaryColor
                                                    : singleton.greyText),
                                          ),
                                        )),
                                    Expanded(
                                        flex: 1,
                                        child: AutoSizeText(
                                          e.rssi.toString(),
                                          style: TextStyle(
                                              color: e.device.name
                                                      .toString()
                                                      .toUpperCase()
                                                      .contains("SAET")
                                                  ? singleton.primaryColor
                                                  : singleton.greyText,
                                              fontWeight: FontWeight.w200),
                                        )),
                                  ],
                                )),
                          )
                        : SizedBox()))
                  ],
                ),
              ),
            ],
          ),
          isScanning
              ? Center(
                  child: SpinKitThreeBounce(
                    color: singleton.primaryColor,
                    size: 40,
                  ),
                )
              : SizedBox(),
          loading
              ? Center(
                  child: CircularProgressIndicator(
                    color: singleton.primaryColor,
                  ),
                )
              : SizedBox()
        ],
      ),
    );
  }
}
