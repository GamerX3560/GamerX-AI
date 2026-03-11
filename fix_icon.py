from PIL import Image

# Open the original image
img = Image.open('/home/gamerx/.gemini/antigravity/brain/7ee45234-8857-4e69-80ce-dbf95d1231b2/gamerx_ai_logo_1772632402956.png').convert("RGBA")

width, height = img.size
# Increase canvas size by 1.6x so the logo sits comfortably inside the adaptive squircle mask
new_size = int(width * 1.6)
new_img = Image.new('RGBA', (new_size, new_size), (0, 0, 0, 0))
offset = ((new_size - width) // 2, (new_size - height) // 2)

new_img.paste(img, offset)

# Save the padded foreground
new_img.save('app/src/main/res/drawable/ic_launcher_foreground.png')
print("Adaptive icon padded foreground saved successfully")
