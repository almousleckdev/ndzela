import 'dart:async';

import 'package:flutter/widgets.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../platform/app_icon.dart';
import '../../platform/app_icons.dart';
import 'platform_icon.dart';

enum AppToastKind { success, error, info }

// A card that slides in from the top, stays a few seconds and leaves; a tap or a swipe up closes it.
class AppToast extends StatefulWidget {
  const AppToast({
    super.key,
    required this.message,
    required this.kind,
    required this.onClosed,
    this.title,
    this.stay = const Duration(seconds: 4),
  });

  final String message;
  final String? title;
  final AppToastKind kind;
  final Duration stay;
  final VoidCallback onClosed;

  @override
  State<AppToast> createState() => _AppToastState();
}

class _AppToastState extends State<AppToast>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 380),
    reverseDuration: const Duration(milliseconds: 240),
  );
  late final Animation<double> _slide = CurvedAnimation(
    parent: _controller,
    curve: Curves.easeOutCubic,
    reverseCurve: Curves.easeInCubic,
  );
  Timer? _timer;
  var _closing = false;

  @override
  void initState() {
    super.initState();
    _controller.forward();
    _timer = Timer(widget.stay, _close);
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _close() async {
    if (_closing) return;
    _closing = true;
    _timer?.cancel();
    await _controller.reverse();
    if (mounted) widget.onClosed();
  }

  (AppIcon, Color, Color) get _look => switch (widget.kind) {
    AppToastKind.success => (
      AppIcons.done,
      AppColors.success,
      AppColors.successSoft,
    ),
    AppToastKind.error => (
      AppIcons.error,
      AppColors.danger,
      AppColors.dangerSoft,
    ),
    AppToastKind.info => (AppIcons.support, AppColors.ink, AppColors.skeleton),
  };

  @override
  Widget build(BuildContext context) {
    final (icon, color, soft) = _look;
    final title = widget.title;
    return Positioned(
      top: MediaQuery.paddingOf(context).top + 10,
      left: 16,
      right: 16,
      child: AnimatedBuilder(
        animation: _slide,
        builder: (context, child) => Opacity(
          opacity: _slide.value,
          child: Transform.translate(
            offset: Offset(0, -28 * (1 - _slide.value)),
            child: child,
          ),
        ),
        child: GestureDetector(
          onTap: _close,
          onVerticalDragEnd: (d) {
            if ((d.primaryVelocity ?? 0) < 0) _close();
          },
          child: Semantics(
            liveRegion: true,
            label: title == null ? widget.message : '$title. ${widget.message}',
            child: ExcludeSemantics(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: AppColors.canvas,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.line),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x1F0F172A),
                      blurRadius: 24,
                      offset: Offset(0, 8),
                    ),
                  ],
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 32,
                        height: 32,
                        decoration: BoxDecoration(
                          color: soft,
                          shape: BoxShape.circle,
                        ),
                        child: Center(
                          child: PlatformIcon(icon, size: 18, color: color),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (title != null)
                              Text(
                                title,
                                style: AppTextStyles.body.copyWith(
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            Text(
                              widget.message,
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                              style: AppTextStyles.label.copyWith(
                                color: AppColors.muted,
                                height: 1.35,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
