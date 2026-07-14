import type { MetadataRoute } from "next";

// ponytail: SVG-only icons (installable in modern Chrome); add PNG 192/512 when a real logo lands.
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Codillas Study Platform",
    short_name: "Codillas",
    description: "Learning platform for the Codillas IT school",
    start_url: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#6d28d9",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml",
        purpose: "any",
      },
    ],
  };
}
