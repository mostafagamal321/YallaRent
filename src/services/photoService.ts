import { Filesystem, Directory } from '@capacitor/filesystem';

/**
 * Resizes a base64 or dataURL image so its width is around 800px max,
 * maintaining aspect ratio, and compressing as JPEG with 0.82 quality.
 */
export async function compressImageTo800px(dataUrl: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const maxW = 800;
      let width = img.width;
      let height = img.height;
      if (width > maxW) {
        height = Math.round((height * maxW) / width);
        width = maxW;
      }
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve(dataUrl);
        return;
      }
      ctx.drawImage(img, 0, 0, width, height);
      const compressed = canvas.toDataURL('image/jpeg', 0.82);
      resolve(compressed);
    };
    img.onerror = (err) => reject(err);
    img.src = dataUrl;
  });
}

/**
 * Takes a picture with the camera or chooses from the gallery.
 * Uses Capacitor Camera plugin if available, otherwise seamlessly uses HTML5 file/camera input.
 * Resizes to ~800px and stores via @capacitor/filesystem (or fallback local URL).
 */
export async function captureOrPickPhoto(source: 'camera' | 'photos'): Promise<string | null> {
  try {
    const CapacitorCamera = (window as any).Capacitor?.Plugins?.Camera;
    if (CapacitorCamera && typeof CapacitorCamera.getPhoto === 'function') {
      const image = await CapacitorCamera.getPhoto({
        quality: 82,
        allowEditing: true,
        resultType: 'dataUrl',
        source: source === 'camera' ? 'CAMERA' : 'PHOTOS',
        width: 800,
        correctOrientation: true,
      });

      if (!image?.dataUrl) return null;
      const compressed = await compressImageTo800px(image.dataUrl);
      return await saveToFilesystem(compressed);
    }
  } catch (err: any) {
    if (err?.message?.includes('User cancelled') || err?.message?.includes('cancelled')) {
      return null;
    }
    console.warn('Capacitor camera failed or unavailable, falling back to web file input:', err);
  }

  // Fallback to web file/camera input
  const webPhoto = await pickPhotoViaWebInput(source);
  if (!webPhoto) return null;
  return await saveToFilesystem(webPhoto);
}

async function saveToFilesystem(dataUrl: string): Promise<string> {
  const fileName = `tenant_photo_${Date.now()}_${Math.floor(Math.random() * 1000)}.jpg`;
  try {
    const base64Data = dataUrl.replace(/^data:image\/[a-z]+;base64,/, '');
    const saved = await Filesystem.writeFile({
      path: `photos/${fileName}`,
      data: base64Data,
      directory: Directory.Data,
      recursive: true,
    });
    return saved.uri || dataUrl;
  } catch (fsErr) {
    console.warn('Filesystem save error, using dataUrl directly:', fsErr);
    return dataUrl;
  }
}

/**
 * Web fallback using HTML file input
 */
function pickPhotoViaWebInput(source: 'camera' | 'photos'): Promise<string | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    if (source === 'camera') {
      input.capture = 'environment';
    }
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) {
        resolve(null);
        return;
      }
      const reader = new FileReader();
      reader.onload = async () => {
        const rawData = reader.result as string;
        try {
          const compressed = await compressImageTo800px(rawData);
          resolve(compressed);
        } catch {
          resolve(rawData);
        }
      };
      reader.onerror = () => resolve(null);
      reader.readAsDataURL(file);
    };
    input.oncancel = () => resolve(null);
    input.click();
  });
}
