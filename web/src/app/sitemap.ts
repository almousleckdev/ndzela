import type { MetadataRoute } from "next";
import { siteConfig } from "@/config/site";

export default function sitemap(): MetadataRoute.Sitemap {
  return ["", "/devenir-chauffeur", "/contact", "/confidentialite", "/conditions", "/suppression-compte"].map((path) => ({ url: `${siteConfig.url}${path}` }));
}
