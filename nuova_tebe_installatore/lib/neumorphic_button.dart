import 'package:flutter/material.dart' hide BoxDecoration, BoxShadow;
import 'package:flutter/services.dart';
import 'package:flutter_inset_box_shadow/flutter_inset_box_shadow.dart';
import 'package:nuova_tebe_installatore/main.dart';

class NeumorphicButton extends StatefulWidget {
  NeumorphicButton(
      {Key? key,
      required this.child,
      required this.action,
      this.padding,
      this.margin,
      this.smaller = false,
      this.borderRadius,
      this.isBreadCrumbs = false,
      this.internal = false,
      this.red_flag = false,
      this.blackBackground = false,
      this.loadingBar = false,
      this.notTappable = false})
      : super(key: key);

  bool loadingBar = false;
  bool isBreadCrumbs = false;
  Widget child;
  BorderRadius? borderRadius = BorderRadius.circular(100);
  final void Function() action;
  bool smaller = false;
  EdgeInsets? padding;
  EdgeInsets? margin;
  bool internal = false;
  bool red_flag = false;
  bool notTappable = false;
  bool blackBackground = false;

  @override
  State<NeumorphicButton> createState() => NeumorphicButtonState();
}

class NeumorphicButtonState extends State<NeumorphicButton> {
  Offset distance = const Offset(10, 10);
  late void Function() action;

  @override
  void initState() {
    if (widget.notTappable) {
      widget.internal = true;
    }
    action = () {
      HapticFeedback.lightImpact();

      print("CAMBIO STATO");
      setState(() {
        widget.internal = !widget.internal;
      });
      Future.delayed(const Duration(milliseconds: 120)).then((value) {
        print("PASSATI 200 ms");
        widget.action();
      });
    };

    super.initState();
    distance = !widget.internal
        ? widget.smaller
            ? const Offset(7, 7)
            : const Offset(10, 10)
        : const Offset(7, 7);
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.max,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Expanded(
          child: GestureDetector(
            onTap: widget.notTappable ? null : action,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              margin:
                  widget.margin ?? (const EdgeInsets.fromLTRB(20, 15, 20, 15)),
              padding:
                  widget.padding ?? (const EdgeInsets.fromLTRB(15, 10, 15, 10)),
              child: widget.child,
              decoration: BoxDecoration(
                borderRadius: widget.borderRadius ?? BorderRadius.circular(100),
                border: widget.red_flag
                    ? Border.all(color: Colors.red.shade800, width: 0.5)
                    : null,
                color: singleton.backgroundColor,
                boxShadow: [
                  BoxShadow(
                      color: widget.internal
                          ? !widget.loadingBar
                              ? Colors.white
                              : Colors.white.withOpacity(0.85)
                          : widget.blackBackground
                              ? Colors.white10
                              : Colors.white.withOpacity(0.85),
                      spreadRadius: widget.internal ? 1.0 : -3,
                      offset: -distance,
                      blurRadius: widget.loadingBar ? 4.0 : 6.0,
                      inset: widget.internal),
                  BoxShadow(
                      color: widget.internal
                          ? widget.loadingBar
                              ? Colors.black.withOpacity(0.32)
                              : Colors.black26
                          : Colors.black.withOpacity(0.12),
                      spreadRadius: widget.internal ? 1.0 : -3,
                      offset: distance,
                      blurRadius: widget.loadingBar ? 4.0 : 6.0,
                      inset: widget.internal),
                ],
              ),
            ),
          ),
        )
      ],
    );
  }
}
