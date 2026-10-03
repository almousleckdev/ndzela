const AVATAR_SIZE = 512;

// Prepares a profile picture in the browser
export async function prepareAvatar(file: File): Promise<File> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    throw new Error("IMAGE_UNREADABLE");
  }

  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement("canvas");
  canvas.width = canvas.height = Math.min(AVATAR_SIZE, side);
  const context = canvas.getContext("2d");
  if (!context) throw new Error("IMAGE_UNREADABLE");
  context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.9));
  if (!blob) throw new Error("IMAGE_UNREADABLE");
  return new File([blob], "avatar.jpg", { type: "image/jpeg" });
}
