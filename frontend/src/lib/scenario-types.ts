// Mirrors docs/lesson-scenario.schema.json (version 1.0.0). Keep in sync with that file — it's
// the single source of truth (see the schema's own header comment).

export interface VoiceLine {
  key: string;
  text: string;
  /**
   * Not present yet: the backend returns the scenario's raw jsonb as-is, and the schema has no
   * audio field. Once real TTS lands, GET .../scenario is expected to enrich each voice line
   * with its audio_assets.file_url (see the TODO on AttemptService.scenario) — this field is
   * here so the player already knows what to do with it when that happens.
   */
  audioUrl?: string;
}

export type CharacterEmotion = "neutral" | "joy" | "surprise" | "mild_fear" | "thinking" | "pride";
export type CharacterPose = "stand" | "walk" | "stop" | "point" | "hold_phone" | "clap";

export interface SceneCharacter {
  id: string;
  emotion: CharacterEmotion;
  pose?: CharacterPose;
}

export interface AssistantHint {
  level: 1 | 2;
  line: VoiceLine;
}

export interface Assistant {
  hints: AssistantHint[];
  re_explain: VoiceLine;
}

interface SceneBase {
  key: string;
  background: string;
  characters: SceneCharacter[];
  props?: string[];
}

export type NarrationAction =
  | "walk_to_road"
  | "stop"
  | "look_left_right"
  | "cross"
  | "pick_phone"
  | "dial"
  | "wave"
  | "none";

export interface NarrationLine {
  line: VoiceLine;
  action?: NarrationAction;
}

export interface DemoScene extends SceneBase {
  type: "demo";
  narration: NarrationLine[];
}

export type ConsequenceAnimation = "car_horn_stop" | "guide_startled" | "guide_oops" | "none";
export type OptionId = "a" | "b" | "c";

export interface ChoiceOption {
  id: OptionId;
  icon: string;
  label: VoiceLine;
  correct: boolean;
  feedback: { line: VoiceLine; consequence_animation?: ConsequenceAnimation };
}

export interface ChoiceScene extends SceneBase {
  type: "choice";
  is_control: boolean;
  setup: VoiceLine;
  options: ChoiceOption[];
  assistant: Assistant;
}

export interface SortingCategory {
  id: "cat_1" | "cat_2";
  icon: string;
  label: VoiceLine;
}

export interface SortingItem {
  icon: string;
  target: "cat_1" | "cat_2";
  feedback_correct: VoiceLine;
  feedback_wrong: VoiceLine;
}

export interface SortingScene extends SceneBase {
  type: "sorting";
  is_control: boolean;
  setup: VoiceLine;
  categories: SortingCategory[];
  items: SortingItem[];
  assistant: Assistant;
}

export interface DialogAnswer {
  id: OptionId;
  icon: string;
  label: VoiceLine;
  correct: boolean;
  feedback: VoiceLine;
}

export interface DialogScene extends SceneBase {
  type: "dialog";
  is_control: boolean;
  question: VoiceLine;
  answers: DialogAnswer[];
  assistant: Assistant;
}

/** Any scene shape the schema doesn't (yet) define — the renderer falls back gracefully. */
export interface UnknownScene extends Partial<SceneBase> {
  type?: string;
  key: string;
  [extra: string]: unknown;
}

export type Scene = DemoScene | ChoiceScene | SortingScene | DialogScene | UnknownScene;

/** Interactive scene types that get answered via POST .../attempts/{id}/answers. */
export type AnswerableScene = ChoiceScene | DialogScene;

export function isChoiceScene(scene: Scene): scene is ChoiceScene {
  return scene.type === "choice";
}

export function isDemoScene(scene: Scene): scene is DemoScene {
  return scene.type === "demo";
}

export function isAnswerableScene(scene: Scene): scene is AnswerableScene {
  return scene.type === "choice" || scene.type === "dialog";
}

export interface LessonScenario {
  schema_version: string;
  module: string;
  topic: string;
  pattern_ids: string[];
  age_band: string;
  title: string;
  learning_goal: string;
  reality_bridge: VoiceLine;
  scenes: Scene[];
}
