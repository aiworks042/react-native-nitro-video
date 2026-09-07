/**
 * A resizing mode for fitting video inside NitroVideoView.
 * - `cover`: Scale the content to fill the size of the view (aspect ratio preserved, cropped if needed).
 * - `contain`: Scale the content to fit the view without cropping (aspect ratio preserved, letterboxed if needed).
 * - `stretch`: Scale the content to fit the size of the view by stretching dimensions.
 */
export type ResizeMode = 'contain' | 'cover' | 'stretch'
