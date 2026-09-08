import { getHostComponent } from 'react-native-nitro-modules'
import NitroVideoViewConfig from '../../nitrogen/generated/shared/json/NitroVideoViewConfig.json'
import type {
  NitroVideoViewProps,
  NitroVideoViewMethods,
} from '../specs/NitroVideoView.nitro'

/**
 * Singleton host component registered with React Native Fabric renderer.
 * Must only be called once across the entire application runtime to prevent
 * "Invariant Violation: Tried to register two views with the same name NitroVideoView".
 */
export const NativeVideoView = getHostComponent<
  NitroVideoViewProps,
  NitroVideoViewMethods
>('NitroVideoView', () => NitroVideoViewConfig)
