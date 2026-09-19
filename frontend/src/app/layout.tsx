import type { Metadata, Viewport } from "next";
import { Nunito, IBM_Plex_Sans, IBM_Plex_Mono } from "next/font/google";
import { ServiceWorkerRegistration } from "@/components/ServiceWorkerRegistration";
import "./globals.css";

// Cyrillic підмножина обов'язкова — весь інтерфейс українською.
const plexSans = IBM_Plex_Sans({
  variable: "--font-plex-sans",
  subsets: ["latin", "cyrillic"],
  weight: ["400", "500", "600"],
});

const plexMono = IBM_Plex_Mono({
  variable: "--font-plex-mono",
  subsets: ["latin", "cyrillic"],
  weight: ["400", "500"],
});

// Дитячий режим (/play/*) — інший шрифт, тому вантажиться поруч, а не замість Plex.
// Baloo 2 не має кириличних гліфів, тому округлий Nunito — найближчий playful варіант з підтримкою української.
const baloo = Nunito({
  variable: "--font-baloo",
  subsets: ["latin", "cyrillic"],
  weight: ["600", "700", "800"],
});

export const metadata: Metadata = {
  title: "Безпечний Простір",
  description: "Навчальна платформа для дошкільнят: кабінет батьків, вчителів та адміністраторів.",
  manifest: "/manifest.webmanifest",
};

export const viewport: Viewport = {
  themeColor: "#0b5560",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="uk"
      className={`${plexSans.variable} ${plexMono.variable} ${baloo.variable} h-full antialiased`}
    >
      <body className="bg-canvas text-ink flex min-h-full flex-col text-base">
        {children}
        <ServiceWorkerRegistration />
      </body>
    </html>
  );
}
