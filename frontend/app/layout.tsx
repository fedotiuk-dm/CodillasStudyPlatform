export const metadata = {
  title: "Codillas Study Platform",
  description: "Learning platform for the Codillas IT school",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="uk">
      <body>{children}</body>
    </html>
  );
}
