import 'dart:async';
import 'dart:io';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:nuova_tebe_installatore/main.dart';
import 'package:nuova_tebe_installatore/neumorphic_button.dart';
import 'dart:io';
import 'dart:typed_data';

class DeviceBE extends StatefulWidget {
  DeviceBE({Key? key, required this.deviceServices, required this.device})
      : super(key: key);

  final List<BluetoothService> deviceServices;
  BluetoothDevice device;

  @override
  State<DeviceBE> createState() => _DeviceBEState();
}

class _DeviceBEState extends State<DeviceBE> {
  List<int> _getBytesSblocco() {
    return [
      67,
      255,
      183,
      123,
    ];
  }

  List<int> _getBytesVersion() {
    return [
      86,
      58,
    ];
  }

  List<int> _getBytesSbloccoFw() {
    return [85, 2, 165, 90];
  }

  final formkey = GlobalKey<FormState>();

  List<BluetoothService> services = [];

  String ritorno = "";
  bool finishedUpdateFw = false;

  @override
  void dispose() {
    widget.device.disconnect();
    super.dispose();
  }

  BluetoothCharacteristic? blc;
  static const platform =
      MethodChannel('com.example.nuova_tebe_installatore/GATTserver');

  int n_puntini = 0;
  Future<void> _onMethodReceive(call) async {
    if (call.method == 'prova') {
      setState(() {
        ritorno = "SONO IN METHOD RECEIVE " + call.arguments.toString();
      });
    } else if (call.method == 'send') {
      if (blc != null) {
        print(call.arguments.toString());
        List<int> bytetoWrite = [];
        bytetoWrite = call.arguments;
        blc!.write(bytetoWrite, withoutResponse: true).then((value) async {
          print(" RES " + value.toString());
          print("ho scritto " + call.arguments.toString());
          setState(() {
            ritorno = "ho scritto " + call.arguments.toString();
          });
        });
      }
    } else if (call.method == 'read_num_impianto') {
      setState(() {
        plantNumber = call.arguments.toString();
        myController.text = "";
        myController.text = call.arguments.toString();
      });
      //LEGGO VERSIONE
      blc!.write(_getBytesVersion(), withoutResponse: true).then((value) async {
        setState(() {
          ritorno = "Lettura versione";
          loading = false;
        });
      });
    } else if (call.method == 'read_flag') {
      setState(() {
        selectedConfig = call.arguments;
      });
    } else if (call.method == 'read_version') {
      setState(() {
        version = call.arguments;
      });
    } else if (call.method == 'scrittura_corretta') {
      setState(() {
        showEsito = true;
        esito = call.arguments;
      });
    } else if (call.method == 'send_fw_chunk') {
      // print(call.arguments.toString());
      List<int> bytetoWrite = [];
      bytetoWrite = call.arguments;
      blc!.write(bytetoWrite, withoutResponse: true).then((value) async {
        // print("ricevuto da fwUpdate " + call.arguments.toString());
        setState(() {
          n_puntini++;
          n_puntini = n_puntini % 9;
          if (n_puntini == 0) {
            ritorno = "Aggiornamento firmware in corso .";
          } else if (n_puntini == 3) {
            ritorno = "Aggiornamento firmware in corso ..";
          } else if (n_puntini == 6) {
            ritorno = "Aggiornamento firmware in corso ...";
          }
        });
      });
    } else if (call.method == 'updatePBar') {
      // print("UPDATE BAR");
      setState(() {
        offset = call.arguments;

        setState(() {
          progressLoading =
              ((MediaQuery.of(context).size.width - 66) * offset) / total;
        });

        if ((offset + 128) >= total) {
          setState(() {
            updating_firmware = false;
            isUpdatingFirmware = false;
            finishedUpdateFw = true;
            print("updating_firmware " +
                updating_firmware.toString() +
                " isUpdatingFirmware:" +
                isUpdatingFirmware.toString() +
                " finishedUpdateFw: " +
                finishedUpdateFw.toString());
          });
        }
      });
    }
  }

  int total = 0;
  int offset = 0;

  String plantNumber = "1";
  int selectedConfig = 0;
  bool loading = false;
  TextEditingController myController = TextEditingController()..text = '';
  bool updating_firmware = false;
  String version = "-1";
  Directory? _downloadsDirectory;
  bool showEsito = false;
  bool esito = true;
  BluetoothDeviceState deviceState = BluetoothDeviceState.connected;

  @override
  void initState() {
    platform.setMethodCallHandler((call) => _onMethodReceive(call));
    for (BluetoothService s in widget.deviceServices) {
      if (!services.contains(s)) {
        services.add(s);
        print("AGGIUNGO SERVIZIO ${s.uuid}");
        if (s.uuid.toString().contains("1ff0100")) {
          print("trovato service corretto");
          setState(() {
            ritorno = "trovato service corretto";
          });
          if (s.characteristics
              .where((element) => element.uuid.toString().contains("1ff010"))
              .isNotEmpty) {
            print("trovata caratteristica");
            setState(() {
              ritorno = "Connesso per configurazione";
            });
            blc = s.characteristics
                .where((element) => element.uuid.toString().contains("01ff010"))
                .first;

            readData();
          }
        }
      }
    }
    listenToStateChange();
    super.initState();
  }

  listenToStateChange() {
    widget.device.state.listen((event) {
      deviceState = event;
      if (deviceState == BluetoothDeviceState.connecting) {
        print("DEVICE IS CONNECTING");
      }
      if (deviceState == BluetoothDeviceState.connected) {
        print("DEVICE IS CONNECTED " + isUpdatingFirmware.toString());
        /*  if (!isUpdatingFirmware) {
          print("finupfw " + finishedUpdateFw.toString());
          print("LEGGO");
          readData();
        }
        */
      }
      if (deviceState == BluetoothDeviceState.disconnecting) {
        print("DEVICE IS DISCONNECTING");
      }
      if (deviceState == BluetoothDeviceState.disconnected) {
        print("DEVICE DISCONNECTED");
        if (isUpdatingFirmware || finishedUpdateFw) {
          widget.device.connect().then((value) async {
            if (!finishedUpdateFw) {
              int mtu = await widget.device.requestMtu(240);
              print("MTU " + mtu.toString());
              sbloccoFirmware();
            } else {
              readData();
            }

            setState(() {
              finishedUpdateFw = false;
            });
          });
        }
      }
    });
  }

  setPlantValue(String value) async {
    await platform.invokeMethod('setPlant', value);
  }

  setFlagValue() async {
    String value = "NFC+125KHz";
    switch (selectedConfig) {
      case 0:
        value = "NFC+125KHz";
        break;
      case 1:
        value = "NFC+125KHz+13MHz";
        break;
      case 2:
        value = "125KHz+13MHz";
        break;
    }

    await platform.invokeMethod('setFlag', value);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: singleton.backgroundColor,
        foregroundColor: singleton.primaryColor,
        title: Text("Aggiornamento lettore"),
        centerTitle: true,
        elevation: 0,
        actions: [
          InkWell(
            onTap: () {
              openPerformanceImpiantiCard(context);
            },
            child: Padding(
              padding: EdgeInsets.only(right: 15),
              child: Icon(Icons.info_outline_rounded),
            ),
          ),
        ],
      ),
      backgroundColor: singleton.backgroundColor,
      body: Stack(
        children: [
          SingleChildScrollView(
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  SizedBox(
                    height: 30,
                  ),
                  Center(
                    child: Text(ritorno),
                  ),
                  if (showEsito)
                    Padding(
                      padding: const EdgeInsets.only(top: 12.0),
                      child: Center(
                          child: esito
                              ? Icon(Icons.check, color: Colors.green, size: 26)
                              : Icon(
                                  Icons.close,
                                  color: Colors.red,
                                  size: 26,
                                )),
                    ),
                  SizedBox(
                    height: showEsito ? 18 : 30,
                  ),
                  Container(
                    width: MediaQuery.of(context).size.width,
                    height: 70,
                    child: Row(
                      children: [
                        Expanded(
                          child: NeumorphicButton(
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
                                children: [
                                  Icon(Icons.book_outlined),
                                  Text("Leggi"),
                                ],
                              ),
                              action: () {
                                readData();
                              }),
                        ),
                        Expanded(
                          child: NeumorphicButton(
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
                                children: [
                                  Icon(Icons.refresh),
                                  Text("Reboot"),
                                ],
                              ),
                              action: () {
                                reboot();
                              }),
                        ),
                      ],
                    ),
                  ),
                  ListTile(
                    title: Text('NFC+125KHz'),
                    leading: Radio<int>(
                      activeColor: singleton.primaryColor,
                      value: 0,
                      groupValue: selectedConfig,
                      onChanged: (int? value) {
                        setState(() {
                          selectedConfig = value ?? 0;
                        });
                      },
                    ),
                  ),
                  ListTile(
                    title: Text('NFC+125KHz+13MHz'),
                    leading: Radio<int>(
                      activeColor: singleton.primaryColor,
                      value: 1,
                      groupValue: selectedConfig,
                      onChanged: (int? value) {
                        setState(() {
                          selectedConfig = value ?? 0;
                        });
                      },
                    ),
                  ),
                  ListTile(
                    title: Text('125KHz+13MHz'),
                    leading: Radio<int>(
                      activeColor: singleton.primaryColor,
                      value: 2,
                      groupValue: selectedConfig,
                      onChanged: (int? value) {
                        setState(() {
                          selectedConfig = value ?? 0;
                        });
                      },
                    ),
                  ),
                  Container(
                      height: 275,
                      width: MediaQuery.of(context).size.width,
                      child: writePlantNumber()),
                  Stack(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 15, 20, 10),
                        child: NeumorphicButton(
                          notTappable: loading,
                          padding: EdgeInsets.fromLTRB(20, 17, 20, 17),
                          action: _updateFirmare,
                          child: Center(
                            child: Text(' Aggiornamento Firmware '),
                          ),
                        ),
                      ),
                      Positioned(
                          top: 0,
                          right: 25,
                          child: InkWell(
                              onTap: () {
                                firmwareInfo(context);
                              },
                              child: Icon(
                                Icons.info_outline_rounded,
                                color: singleton.primaryColor,
                                size: 25,
                              )))
                    ],
                  ),
                  Center(
                      child: Text(
                    "Versione: $version",
                    style: TextStyle(color: singleton.primaryColor),
                  )),
                  if (updating_firmware) loadingBar()
                ],
              ),
            ),
          ),
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

  reboot() async {
    await platform.invokeMethod('setStatus', "REBOOT");
    if (blc != null) {
      blc!.write(_getBytesSblocco(), withoutResponse: true).then((value) async {
        setState(() {
          ritorno = "Reboot in corso";
        });
      });
    }
  }

  readData() async {
    setState(() {
      showEsito = false;
    });
    await platform.invokeMethod('setStatus', "SEND ENABLE CONF READ");
    if (blc != null) {
      blc!.write(_getBytesSblocco(), withoutResponse: true).then((value) async {
        setState(() {
          ritorno = "Configurazione in corso";
        });
      });
    }
  }

  double progressLoading = 20.0;

  Widget loadingBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(30.0, 10, 30, 10),
      child: SizedBox(
        width: MediaQuery.of(context).size.width,
        child: NeumorphicButton(
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.all(3),
          action: () {},
          notTappable: true,
          internal: true,
          loadingBar: true,
          child: Stack(
            children: [
              SizedBox(
                width: MediaQuery.of(context).size.width,
                height: 10,
              ),
              Padding(
                padding: const EdgeInsets.all(1),
                child: AnimatedContainer(
                  height: 8,
                  width: progressLoading,
                  decoration: BoxDecoration(
                      color: singleton.primaryColor,
                      borderRadius: BorderRadius.circular(80)),
                  duration: Duration(milliseconds: 250),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget writePlantNumber() {
    return Form(
      key: formkey,
      child: Container(
        padding: const EdgeInsets.fromLTRB(40, 20, 40, 25),
        child: Column(
          children: [
            TextFormField(
              controller: myController,
              decoration: InputDecoration(
                  labelText: 'INST',
                  focusedBorder: UnderlineInputBorder(
                      borderSide: BorderSide(color: singleton.primaryColor))),
              validator: (input) => input!.isEmpty ? "Plant non valido" : null,
              onSaved: (input) => plantNumber = input!.trim(),
            ),
            const SizedBox(height: 50),
            NeumorphicButton(
              notTappable: loading,
              padding: EdgeInsets.fromLTRB(20, 17, 20, 17),
              // margin: EdgeInsets.fromLTRB(5, 12, 5, 10),
              action: _sendConfig,
              child: Center(
                child: Text(' Invia configurazione '),
              ),
            ),
          ],
        ),
      ),
    );
  }

  _sendConfig() async {
    if (formkey.currentState!.validate()) {
      formkey.currentState!.save();
      setState(() {
        loading = true;
      });
      await setPlantValue(plantNumber);
      await setFlagValue();
      await platform.invokeMethod('setStatus', "SEND ENABLE CONF");

      blc!.write(_getBytesSblocco(), withoutResponse: true).then((value) async {
        setState(() {
          ritorno = "Configurazione in corso";
          loading = false;
        });
      });
    }
  }

  bool isUpdatingFirmware = false;

  sbloccoFirmware() async {
    if (blc != null)
      await blc!.write(_getBytesSbloccoFw(), withoutResponse: true);
  }

  _updateFirmare() async {
    setState(() {
      isUpdatingFirmware = true;
    });
    var bytes = await inputDatePickerFormField();
    setState(() {
      total = bytes!.length;
    });
    int mtu = await widget.device.requestMtu(240);
    print("MTU " + mtu.toString());
    await platform.invokeMethod('setStatus', "SEND FIRMWARE");
    await platform.invokeMethod('setFile', bytes.toString());
    await sbloccoFirmware();
    setState(() {
      updating_firmware = true;
      progressLoading = 0;
    });

    ///CONSIDERIAMO BARRA LUN TOTALE=  MediaQuery.of(context).size.width-66
  }

  openPerformanceImpiantiCard(BuildContext context) {
    showDialog(
        context: context,
        builder: (BuildContext context) => Container(
            width: MediaQuery.of(context).size.width,
            child: Dialog(
                backgroundColor: Colors.transparent,
                child: Container(
                  height: 250,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(30),
                    color: singleton.backgroundColor,
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Center(
                          child: Text(
                        widget.device.name.toString(),
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: singleton.primaryColor,
                            fontSize: 21),
                      )),
                      Padding(
                        padding: const EdgeInsets.only(top: 20.0),
                        child: Column(
                          children: [
                            Text(
                              "MAC Address",
                              style: TextStyle(
                                  color: singleton.greyText,
                                  fontWeight: FontWeight.w200),
                            ),
                            Text(widget.device.id.toString()),
                          ],
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.only(top: 20.0, bottom: 10),
                        child: Column(
                          children: [
                            Text(
                              "Bluetooth type",
                              style: TextStyle(
                                  color: singleton.greyText,
                                  fontWeight: FontWeight.w200),
                            ),
                            Text(widget.device.type.toString()),
                          ],
                        ),
                      )
                    ],
                  ),
                ))));
  }

  firmwareInfo(BuildContext context) {
    showDialog(
        context: context,
        builder: (BuildContext context) => Container(
            width: MediaQuery.of(context).size.width,
            child: Dialog(
                backgroundColor: Colors.transparent,
                child: Container(
                  height: 170,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(30),
                    color: singleton.backgroundColor,
                  ),
                  child: Center(
                    child: Padding(
                      padding: const EdgeInsets.all(20.0),
                      child: AutoSizeText(
                        "Per aggiornare il firmware dovrai accedere ai tuoi file e selezionare il rispettivo file di configurazione del firmware; se la selezione a buon fine, l'aggiornamento partirà automaticamente.",
                        textAlign: TextAlign.center,
                        style: TextStyle(color: singleton.primaryColor),
                      ),
                    ),
                  ),
                ))));
  }

  Future<Uint8List?> inputDatePickerFormField() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['bin'],
    );

    if (result != null) {
      File file = File(result.files.single.path!);
      Uint8List bytes = file.readAsBytesSync();
      print("STAMPO BYTE FW");
      print(bytes.toString());
      print("BYTE LENGHT " + bytes.length.toString());

      return bytes;
    } else {
      return null;
    }
  }
}
