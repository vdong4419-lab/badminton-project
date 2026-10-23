import { createContext, useContext } from 'react';

const PhienContext = createContext(null);
export default PhienContext;
export function usePhienDangNhap() { return useContext(PhienContext); }
