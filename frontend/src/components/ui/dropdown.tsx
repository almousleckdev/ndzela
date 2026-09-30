"use client";

import * as React from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/utils";

interface DropdownProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
  align?: "left" | "right";
  className?: string;
}

interface Position {
  top: number;
  left: number;
  minWidth: number;
}

const MENU_GAP = 8;

/**
 * Menu attached to a trigger. The menu is rendered in a portal with fixed positioning, so it is never
 * clipped by a scrolling table, a card with overflow hidden, or a dialog.
 */
export function Dropdown({ trigger, children, align = "right", className }: DropdownProps) {
  const [position, setPosition] = React.useState<Position | null>(null);
  const triggerRef = React.useRef<HTMLDivElement>(null);
  const menuRef = React.useRef<HTMLDivElement>(null);
  const isOpen = position !== null;

  const close = React.useCallback(() => setPosition(null), []);

  const open = () => {
    const rect = triggerRef.current?.getBoundingClientRect();
    if (!rect) return;
    setPosition({ top: rect.bottom + MENU_GAP, left: align === "right" ? rect.right : rect.left, minWidth: rect.width });
  };

  React.useEffect(() => {
    if (!isOpen) return;
    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (!triggerRef.current?.contains(target) && !menuRef.current?.contains(target)) close();
    };
    const onKey = (event: KeyboardEvent) => event.key === "Escape" && close();
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKey);
    window.addEventListener("resize", close);
    window.addEventListener("scroll", close, true);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("resize", close);
      window.removeEventListener("scroll", close, true);
    };
  }, [isOpen, close]);

  // Flip above the trigger when the menu would run off the bottom of the screen.
  React.useLayoutEffect(() => {
    const menu = menuRef.current;
    const rect = triggerRef.current?.getBoundingClientRect();
    if (!isOpen || !menu || !rect) return;
    if (position.top + menu.offsetHeight > window.innerHeight - MENU_GAP) {
      menu.style.top = `${Math.max(MENU_GAP, rect.top - MENU_GAP - menu.offsetHeight)}px`;
    }
  }, [isOpen, position]);

  return (
    <div className="relative inline-block text-left">
      <div ref={triggerRef} onClick={() => (isOpen ? close() : open())} className="cursor-pointer" aria-haspopup="menu" aria-expanded={isOpen}>
        {trigger}
      </div>

      {isOpen &&
        createPortal(
          <div
            ref={menuRef}
            role="menu"
            onClick={close}
            style={{
              position: "fixed",
              top: position.top,
              left: position.left,
              minWidth: Math.max(position.minWidth, 224),
              transform: align === "right" ? "translateX(-100%)" : undefined,
            }}
            className={cn("z-[60] max-w-[calc(100vw-1rem)] rounded-lg bg-white py-1 shadow-xl ring-1 ring-slate-900/10", className)}
          >
            {children}
          </div>,
          document.body
        )}
    </div>
  );
}

export function DropdownItem({
  children,
  onClick,
  className,
  destructive,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
  destructive?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "block w-full px-4 py-2 text-left text-sm transition-colors",
        destructive ? "text-red-600 hover:bg-red-50 hover:text-red-700" : "text-slate-700 hover:bg-slate-100 hover:text-slate-900",
        className
      )}
      role="menuitem"
    >
      {children}
    </button>
  );
}
