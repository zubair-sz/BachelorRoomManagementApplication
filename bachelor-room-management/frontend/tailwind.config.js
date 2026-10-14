/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#f0f5ff",
          100: "#dbe6fe",
          500: "#4f6df5",
          600: "#3d55d6",
          700: "#2f42a8",
        },
      },
    },
  },
  plugins: [],
};
