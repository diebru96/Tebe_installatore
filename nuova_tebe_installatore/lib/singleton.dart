import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:convert';


class Singleton {
  static Singleton? _instance;
  Color primaryColor = const Color(0xff334556);
  Color primaryColorLight = const Color(0xff4377A8);
  Color primaryColorDark = const Color(0xff2A3540);
  Color accentColor = const Color(0xff4377A8);
  Color backgroundColor = const Color(0xFFecf0f3);
  Color greyText = const Color(0xFF8897a4);
  Color super_light_greyText = const Color.fromARGB(255, 203, 217, 230);


  static get instance {
    _instance ??= Singleton._internal();

    return _instance;
  }



  Singleton._internal();

}