"use client";

import { useParams } from "next/navigation";
import { KidAudioProvider } from "@/components/scene-engine/KidAudio";
import { KidModeProvider } from "@/components/kid/KidMode";

/**
 * Child mode for one child: the shelf, the sticker album and the lessons share one unlocked audio
 * element and one shell (start tap, orientation, protected exit) across client-side navigation.
 */
export default function KidModeLayout({ children }: { children: React.ReactNode }) {
  const { childId } = useParams<{ childId: string }>();
  return (
    <KidAudioProvider>
      <KidModeProvider childId={childId}>{children}</KidModeProvider>
    </KidAudioProvider>
  );
}
