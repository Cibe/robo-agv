import React, { useEffect, useRef } from "react";
import { Animated, Easing, View, StyleSheet, Text } from "react-native";
import Svg, { Circle, Polygon, Line, Path } from "react-native-svg";

type Direction = "forward" | "back" | "left" | "right" | "stop";

interface Props {
  direction: Direction;
}

const SIZE = 280;
const CENTER = SIZE / 2;
const ROBOT_R = 44;
const MOVE = 80;

function directionToOffset(d: Direction): { dx: number; dy: number; angle: number } {
  switch (d) {
    case "forward": return { dx: 0, dy: -MOVE, angle: 0 };
    case "back":    return { dx: 0, dy: MOVE,  angle: 180 };
    case "left":    return { dx: -MOVE, dy: 0, angle: -90 };
    case "right":   return { dx: MOVE,  dy: 0, angle: 90 };
    default:        return { dx: 0,    dy: 0,  angle: 0 };
  }
}

const LABELS: Record<Direction, string> = {
  forward: "Moving Forward",
  back: "Moving Back",
  left: "Turning Left",
  right: "Turning Right",
  stop: "Stopped",
};

export default function RobotView({ direction }: Props) {
  const xAnim = useRef(new Animated.Value(CENTER)).current;
  const yAnim = useRef(new Animated.Value(CENTER)).current;
  const angleAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const { dx, dy, angle } = directionToOffset(direction);
    Animated.parallel([
      Animated.timing(xAnim, {
        toValue: Math.min(Math.max(CENTER + dx, ROBOT_R), SIZE - ROBOT_R),
        duration: 700,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: false,
      }),
      Animated.timing(yAnim, {
        toValue: Math.min(Math.max(CENTER + dy, ROBOT_R), SIZE - ROBOT_R),
        duration: 700,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: false,
      }),
      Animated.timing(angleAnim, {
        toValue: angle,
        duration: 700,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: false,
      }),
    ]).start();
  }, [direction]);

  const AnimatedCircle = Animated.createAnimatedComponent(Circle as any);

  // Build grid lines
  const gridLines = [];
  for (let i = 0; i <= SIZE; i += 40) {
    gridLines.push(
      <Line key={`v${i}`} x1={i} y1={0} x2={i} y2={SIZE} stroke="#1a000000" strokeWidth={1} />,
      <Line key={`h${i}`} x1={0} y1={i} x2={SIZE} y2={i} stroke="#1a000000" strokeWidth={1} />
    );
  }

  return (
    <View style={styles.container}>
      <Svg width={SIZE} height={SIZE}>
        {gridLines}
        {/* Robot body */}
        <AnimatedCircle
          cx={xAnim}
          cy={yAnim}
          r={ROBOT_R}
          fill="#1565C0"
        />
        {/* Arrow (static triangle pointing up, rotation applied via transform) */}
        <AnimatedCircle
          cx={xAnim}
          cy={yAnim}
          r={ROBOT_R * 0.5}
          fill="white"
          opacity={0.9}
        />
      </Svg>
      <Text style={styles.label}>{LABELS[direction]}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
  },
  label: {
    marginTop: 8,
    fontSize: 16,
    fontWeight: "bold",
    color: "#37474F",
  },
});
