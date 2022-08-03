import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:nuova_tebe_installatore/scanPage.dart';

import 'singleton.dart';

void main() {
  runApp(const MyApp());
}

final Singleton singleton = Singleton.instance;

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);

    return MaterialApp(
        title: 'Flutter Demo',
        theme: ThemeData(
          textTheme: GoogleFonts.manropeTextTheme(),
          primarySwatch: Colors.blue,
        ),
        home: ScanPage());
  }
}
