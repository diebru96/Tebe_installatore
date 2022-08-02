import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

class DeviceBE extends StatefulWidget{
    DeviceBE({Key? key, required this.deviceServices, required this.device}) : super(key: key);

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

    final formkey = GlobalKey<FormState>();



  List<BluetoothService> services=[];

  String ritorno="";

  @override
  void dispose() {
    widget.device.disconnect();
    super.dispose();
  }
  BluetoothCharacteristic? blc;
    static const platform = MethodChannel('com.example.nuova_tebe_installatore/GATTserver');

    Future<void> _onMethodReceive(call) async {
        if (call.method == 'prova') {
          setState(() {
            ritorno="SONO IN METHOD RECEIVE "+ call.arguments.toString();
          });
        }

  if(call.method == 'send'){
          if(blc!=null){
            print("sono in send (method receive)");
            print(call.arguments.toString());
List<int> bytetoWrite=[];
bytetoWrite=call.arguments;
            blc!.write(bytetoWrite, withoutResponse: true).then((value) async {
                      print("ho scritto "+ call.arguments.toString());
          setState(() {
            ritorno="ho scritto " + call.arguments.toString();
          });
            });
            }
        }
      }
    

    String plantNumber="1";
  


  @override
  void initState() { 


  platform.setMethodCallHandler((call) => _onMethodReceive(call));
       for (BluetoothService s in widget.deviceServices) {
      if(!services.contains(s))
      {
        services.add(s);
        print("AGGIUNGO SERVIZIO ${s.uuid}");
        if(s.uuid.toString().contains("1ff0100")){
          print("trovato service corretto");
          setState(() {
            ritorno="trovato service corretto";
          });
          if(s.characteristics.where((element) => element.uuid.toString().contains("1ff010")).isNotEmpty){
            print("trovata caratteristica");
          setState(() {
            ritorno="trovata caratteristica";
          });
          

          
          
          blc= s.characteristics.where((element) => element.uuid.toString().contains("01ff010")).first;

          
          
          }
        }
      }




       };

       
    super.initState();

  }

  

  setPlantValue(String value) async {
        await platform.invokeMethod('setPlant', value);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(body: Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Center(child: Text(ritorno),),

Container(
  height: 300,
width: MediaQuery.of(context).size.width,
  child: writePlantNumber()),
          

         
          FlatButton(onPressed: () async {

if(blc!=null){

            blc!.write(_getBytesSblocco(), withoutResponse: true).then((value) async {
                      print("ho scritto enable config");
          setState(() {
            ritorno="ho scritto enable config";
          });

          }
          );
}
          }, child: Text("START DOING SHIT"))
        ],
      ),
    ),);
  }


 Widget writePlantNumber(){
   return Form(
                key: formkey,
                child: Container(
                  padding: const EdgeInsets.all(40),
                  child: Column(
                    children: [
                      TextFormField(
                        decoration: InputDecoration(
                            labelText: 'PLANT',
                            focusedBorder: UnderlineInputBorder(
                                borderSide:
                                    BorderSide(color: Colors.indigo))),
                        validator: (input) =>
                            input!.isEmpty
                                ? "Plant non valido"
                                : null,
                        onSaved: (input) => plantNumber = input!.trim(),
                      ),
                      const SizedBox(height: 30),
                     
                      const SizedBox(height: 45),
                      InkWell(
                        onTap: (){
                              if (formkey.currentState!.validate()) 
                              {
      formkey.currentState!.save();
                setPlantValue(plantNumber);
      }

                        },
                     
                        child: Container(
                          width: 300,
                          height:60,
                          decoration: BoxDecoration(
                                                      color: Colors.grey.shade300,

                            borderRadius: BorderRadius.circular(80)),
                          child: Center(
                            child: Text(' Imposta plant '),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
  }

}

